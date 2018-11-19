package skynet.cluster.actors

import java.util

import akka.actor.{AbstractActor, ActorRef, Props, Terminated}
import akka.event.Logging
import skynet.cluster.actors.Profiler.TaskMessage

object Profiler { ////////////////////////
  // Actor Construction //
  ////////////////////////
  val DEFAULT_NAME = "profiler"

  def props: Props = Props.create(classOf[Profiler])

  ////////////////////
  // Actor Messages //
  ////////////////////
  @SerialVersionUID(4545299661052078209L)
  case class RegistrationMessage() {}

  @SerialVersionUID(-8330958742629706627L)
  case class TaskMessage(attributes: Int = 0) {

  }

  @SerialVersionUID(-6823011111281387872L)
  case class CompletionMessage(result: CompletionMessage.CompletionStatus.CompletionStatus) {

  }

  object CompletionMessage {

    object CompletionStatus extends Enumeration {
      type CompletionStatus = Value
      val MINIMAL, EXTENDABLE, FALSE, FAILED = Value
    }

  }

}

class Profiler extends AbstractActor {
  /////////////////
  // Actor State //
  /////////////////
  final private val log = Logging.getLogger(getContext.system, this)
  final private val unassignedWork = new util.LinkedList[Worker.WorkMessage]
  final private val idleWorkers = new util.LinkedList[ActorRef]
  final private val busyWorkers = new util.HashMap[ActorRef, Worker.WorkMessage]
  private var task: TaskMessage = _

  // Actor Behavior //
  override def createReceive: AbstractActor.Receive =
    receiveBuilder
      .`match`(classOf[Profiler.RegistrationMessage], handleRegistration)
      .`match`(classOf[Terminated], handleTermination)
      .`match`(classOf[Profiler.TaskMessage], handleTask)
      .`match`(classOf[Profiler.CompletionMessage], handleCompletion)
      .matchAny((`object`: Any) => this.log.info("Received unknown message: \"{}\"", `object`.toString))
      .build

  private def handleRegistration(message: Profiler.RegistrationMessage): Unit = {
    this.context.watch(sender)
    this.assign(sender)
    this.log.info("Registered {}", sender)
  }

  private def assign(worker: ActorRef): Unit = {
    val work = unassignedWork.poll
    if (work == null) {
      this.idleWorkers.add(worker)
      return
    }
    this.busyWorkers.put(worker, work)
    worker.tell(work, self)
  }

  private def handleTermination(message: Terminated): Unit = {
    this.context.unwatch(message.getActor)
    if (!this.idleWorkers.remove(message.getActor)) {
      val work = this.busyWorkers.remove(message.getActor)
      if (work != null) this.assign(work)
    }
    this.log.info("Unregistered {}", message.getActor)
  }

  private def assign(work: Worker.WorkMessage): Unit = {
    val worker = idleWorkers.poll
    if (worker == null) {
      unassignedWork.add(work)
      return
    }
    this.busyWorkers.put(worker, work)
    worker.tell(work, self)
  }

  private def handleTask(message: Profiler.TaskMessage): Unit = {
    if (this.task != null)
      this.log.error("The profiler actor can process only one task in its current implementation!")

    this.task = message
    this.assign(Worker.WorkMessage(new Array[Int](0), new Array[Int](0)))
  }

  private def handleCompletion(message: Profiler.CompletionMessage): Unit = {
    val worker = this.sender
    val work = this.busyWorkers.remove(worker)
    log.info("Completed: [{},{}]", util.Arrays.toString(work.x), util.Arrays.toString(work.y))
    import skynet.cluster.actors.Profiler.CompletionMessage.CompletionStatus._
    message match {
      case MINIMAL =>
        report(work)
      case EXTENDABLE =>
        split(work)
      case FALSE =>
      // Ignore
      case FAILED =>
        assign(work)
    }
    this.assign(worker)
  }

  private def report(work: Worker.WorkMessage): Unit = {
    this.log.info("UCC: {}", util.Arrays.toString(work.x))
  }

  private def split(work: Worker.WorkMessage): Unit = {
    val x = work.x
    val y = work.y
    val next = x.length + y.length
    if (next < task.attributes - 1) {
      val xNew = util.Arrays.copyOf(x, x.length + 1)
      xNew(x.length) = next
      assign(Worker.WorkMessage(xNew, y))
      val yNew = util.Arrays.copyOf(y, y.length + 1)
      yNew(y.length) = next
      assign(Worker.WorkMessage(x, yNew))
    }
  }
}
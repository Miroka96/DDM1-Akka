package skynet.cluster.actors

import java.util

import akka.actor.{AbstractActor, ActorRef, Props, Terminated}
import akka.event.Logging
import skynet.cluster.actors.WorkManager.TaskMessage

// distributes work
object WorkManager {
  ////////////////////////
  // Actor Construction //
  ////////////////////////
  val DEFAULT_NAME = "profiler"

  def props: Props = Props.create(classOf[WorkManager])

  ////////////////////
  // Actor Messages //
  ////////////////////
  @SerialVersionUID(4545299661052078209L)
  case class RegistrationMessage()

  @SerialVersionUID(-8330958742629706627L)
  case class TaskMessage(attributes: Int = 0)

  @SerialVersionUID(-6823011111281387872L)
  case class CompletionMessage(result: CompletionMessage.CompletionStatus)

  object CompletionMessage extends Enumeration {
    type CompletionStatus = Value
    val MINIMAL, EXTENDABLE, FALSE, FAILED = Value
  }


}

class WorkManager extends AbstractActor {
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
      .`match`(classOf[WorkManager.RegistrationMessage], handleRegistration)
      .`match`(classOf[Terminated], handleTermination)
      .`match`(classOf[WorkManager.TaskMessage], handleTask)
      .`match`(classOf[WorkManager.CompletionMessage], handleTaskCompletion)
      .matchAny((`object`: Any) => log.info("Received unknown message: \"{}\"", `object`.toString))
      .build

  private def handleRegistration(message: WorkManager.RegistrationMessage): Unit = {
    context.watch(sender)
    assignWorker(sender)
    log.info("Registered {}", sender)
  }

  private def assignWorker(worker: ActorRef): Unit = {
    val work = unassignedWork.poll
    if (work == null) {
      idleWorkers.add(worker)
      return
    }
    busyWorkers.put(worker, work)
    worker.tell(work, self)
  }

  private def handleTermination(message: Terminated): Unit = {
    context.unwatch(message.getActor)
    if (!idleWorkers.remove(message.getActor)) {
      val work = busyWorkers.remove(message.getActor)
      if (work != null) assignWork(work)
    }
    log.info("Unregistered {}", message.getActor)
  }

  private def assignWork(work: Worker.WorkMessage): Unit = {
    val worker = idleWorkers.poll
    if (worker == null) {
      unassignedWork.add(work)
      return
    }
    busyWorkers.put(worker, work)
    worker.tell(work, self)
  }

  private def handleTask(message: WorkManager.TaskMessage): Unit = {
    if (task != null)
      log.error("The profiler actor can process only one task in its current implementation!")

    task = message
    assignWork(Worker.WorkMessage(new Array[Int](0), new Array[Int](0)))
  }

  private def handleTaskCompletion(message: WorkManager.CompletionMessage): Unit = {
    val worker = sender
    val work = busyWorkers.remove(worker)
    log.info("Completed: [{},{}]", util.Arrays.toString(work.x), util.Arrays.toString(work.y))
    import skynet.cluster.actors.WorkManager.CompletionMessage._
    message.result match {
      case MINIMAL =>
        reportWorkResults(work)
      case EXTENDABLE =>
        splitWorkPackage(work)
      case FALSE =>
      // Ignore
      case FAILED =>
        assignWork(work)
    }
    assignWorker(worker)
  }

  private def reportWorkResults(work: Worker.WorkMessage): Unit = {
    log.info("UCC: {}", util.Arrays.toString(work.x))
  }

  private def splitWorkPackage(work: Worker.WorkMessage): Unit = {
    val x = work.x
    val y = work.y
    val next = x.length + y.length
    if (next < task.attributes - 1) {
      val xNew = util.Arrays.copyOf(x, x.length + 1)
      xNew(x.length) = next
      assignWork(Worker.WorkMessage(xNew, y))
      val yNew = util.Arrays.copyOf(y, y.length + 1)
      yNew(y.length) = next
      assignWork(Worker.WorkMessage(x, yNew))
    }
  }
}
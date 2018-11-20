package skynet.cluster.actors

import akka.actor.{AbstractActor, ActorRef, Props, Terminated}
import akka.cluster.Member
import akka.event.Logging
import skynet.cluster.SkynetMaster

// once per Master
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

}

class WorkManager extends AbstractActor {
  /////////////////
  // Actor State //
  /////////////////
  final private val log = Logging.getLogger(getContext.system, this)
  final private val unassignedWork = new java.util.LinkedList[WorkMessage]
  final private val idleWorkers = new java.util.LinkedList[ActorRef]
  final private val busyWorkers = new java.util.HashMap[ActorRef, WorkMessage]
  private var task: TaskMessage = _

  // Actor Behavior //
  override def createReceive: AbstractActor.Receive =
    receiveBuilder
      .`match`(classOf[WorkManager.RegistrationMessage], handleRegistration)
      .`match`(classOf[Terminated], handleTermination)
      .`match`(classOf[TaskMessage], handleTask)
      .`match`(classOf[ResultMessage], handleTaskResult)
      .matchAny((`object`: Any) => log.info("Received unknown message: \"{}\"", `object`.toString))
      .build

  private def handleRegistration(message: WorkManager.RegistrationMessage): Unit = {
    context.watch(sender)
    assignWorker(sender)
    log.info("Registered {}", sender)
  }

  private def handleTask(message: TaskMessage): Unit = {
    if (task != null)
      log.error("The profiler actor can process only one task in its current implementation!")

    task = message
    assignWork(null)
  }

  private def handleTermination(message: Terminated): Unit = {
    context.unwatch(message.getActor)
    if (!idleWorkers.remove(message.getActor)) {
      val work = busyWorkers.remove(message.getActor)
      if (work != null) assignWork(work)
    }
    log.info("Unregistered {}", message.getActor)
  }

  private def assignWork(work: WorkMessage): Unit = {
    val worker = idleWorkers.poll
    if (worker == null) {
      unassignedWork.add(work)
      return
    }
    busyWorkers.put(worker, work)
    worker.tell(work, self)
  }

  private def handleTaskResult(message: ResultMessage): Unit = {
    val worker = sender
    val work = busyWorkers.remove(worker)
    log.info("Completed: {}", work)

    assignWorker(worker)
  }

  private def assignWorker(worker: ActorRef): Unit = {
    val work = unassignedWork.poll()
    if (work == null) {
      idleWorkers.add(worker)
      return
    }

    busyWorkers.put(worker, work)
    worker.tell(work, self)
  }

  private def reportWorkResults(work: WorkMessage): Unit = {
    log.info("UCC: {}", work)
  }

}

trait RegistrationProcess extends AbstractActor {
  protected def eventuallyRegister(member: Member): Unit = {
    if (member.hasRole(SkynetMaster.MASTER_ROLE)) registerAtManager(member)
  }

  protected def registerAtManager(master: Member): Unit = {
    getContext.actorSelection(master.address + "/user/" + WorkManager.DEFAULT_NAME)
      .tell(new WorkManager.RegistrationMessage, self)
  }
}


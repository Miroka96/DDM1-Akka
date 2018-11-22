package skynet.cluster.actors

import akka.actor.{Actor, ActorRef, ActorSelection, Props, Terminated}
import akka.cluster.Member
import skynet.cluster.SkynetMaster
import skynet.cluster.actors.WorkManager.{RegistrationMessage, WelcomeMessage}
import skynet.cluster.actors.util.ErrorHandling

import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer

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

  final case class WelcomeMessage(systemIdentifier: String, workerCount: Int)

}

class WorkManager extends Actor with ErrorHandling {
  /////////////////
  // Actor State //
  /////////////////
  final private val unassignedWork = new java.util.LinkedList[WorkMessage]
  final private val idleWorkers = new ArrayBuffer[ActorRef]
  final private val busyWorkers = new java.util.HashMap[ActorRef, WorkMessage]
  final private var currentTask: TaskState = _
  final private val expectedWorkers = new mutable.HashMap[String, Int]

  private var waiting = true


  // Actor Behavior //
  override def receive: Receive = {
    case _: RegistrationMessage => handleRegistration()
    case m: WelcomeMessage => handleWelcome(m)
    case m: Terminated => handleTermination(m)
    case m: TaskMessage => handleTask(m)
    case m: ResultMessage => handleTaskResult(m)
    case m: Int => println(m, "deine Muddda")
    case m => messageNotUnderstood(m)
  }

  private def handleRegistration(): Unit = {
    context.watch(sender)

    idleWorkers.append(sender)

    if (idleWorkers.size >= expectedWorkers.values.sum) {
     for (worker <- idleWorkers) {
        assignWorker(worker)
      }
    }

    log.info("Registered {}", sender)
  }

  def handleWelcome(m: WelcomeMessage): Unit = {
    println(m)
    println("randalleeeeee")
    expectedWorkers.put(m.systemIdentifier, m.workerCount)
  }

  private def handleTask(message: TaskMessage): Unit = {
    currentTask = message.toProcessingState

    // TODO
    //assignWork(null)
  }

  private def handleTermination(message: Terminated): Unit = {
    context.unwatch(message.getActor)

    if (!idleWorkers.contains(message.getActor)) {
      val work = busyWorkers.remove(message.getActor)
      if (work != null) assignWork(work)
    } else {
      idleWorkers -= message.getActor
    }
    log.info("Unregistered {}", message.getActor)
  }

  private def assignWork(work: WorkMessage): Unit = {
    val worker = idleWorkers.remove(0)
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
      idleWorkers += worker
      return
    }

    busyWorkers.put(worker, work)
    worker.tell(work, self)
  }

  private def reportWorkResults(work: WorkMessage): Unit = {
    log.info("UCC: {}", work)
  }

}

trait RegistrationProcess extends Actor {
  private var _master: Member = _
  var workManager: ActorSelection = _

  def master: Member = _master

  def master_=(value: Member): Unit = {
    _master = value
    this.workManager = context.actorSelection(master.address + "/user/" + WorkManager.DEFAULT_NAME)
    masterFound()
  }

  protected def masterFound(): Unit = {}

  protected def eventuallyRegister(member: Member): Unit = {
    if (member.hasRole(SkynetMaster.MASTER_ROLE)) registerAtManager(member)
  }

  protected def registerAtManager(master: Member): Unit = {
    this.master = master

    workManager.tell(new WorkManager.RegistrationMessage, self)
  }
}


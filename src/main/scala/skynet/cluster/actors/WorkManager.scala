package skynet.cluster.actors

import akka.actor.{Actor, ActorRef, ActorSelection, Props, Terminated}
import akka.cluster.Member
import skynet.cluster.SkynetMaster
import skynet.cluster.actors.util.ErrorHandling
import skynet.cluster.util.WorkerPool
import WorkManager._
import skynet.cluster.actors.Messages.PasswordCrackingMessage
import skynet.cluster.actors.jobs.PasswordJob

// once per Master
object WorkManager {
  ////////////////////////
  // Actor Construction //
  ////////////////////////
  val DEFAULT_NAME = "profiler"

  def props(localWorkers: Int, slaveNodeCount: Int, dataSet: Array[CSVPerson]): Props =
    Props(new WorkManager(localWorkers, slaveNodeCount, dataSet))

  ////////////////////
  // Actor Messages //
  ////////////////////
  case class RegistrationMessage()

  case class WelcomeMessage(systemIdentifier: String, workerCount: Int)

  case class CSVPerson(id: Int, name: String, passwordhash: String, gene: String)

  case class ResultMessage()


}


class WorkManager(localWorkerCount: Int, slaveNodeCount: Int, dataSet: Array[CSVPerson]) extends Actor with ErrorHandling {


  /////////////////
  // Actor State //
  /////////////////
  //final private val unassignedWork = new java.util.LinkedList[WorkMessage]
  //final private val busyWorkers = new java.util.HashMap[ActorRef, WorkMessage]
  private val workerPool = new WorkerPool(slaveNodeCount, localWorkerCount)


  // Actor Behavior //
  override def receive: Receive = {
    case _: RegistrationMessage => handleRegistration()
    case m: WelcomeMessage => handleWelcome(m)
    case m: Terminated => handleTermination(m)
   // case m: ResultMessage => handleTaskResult(m)
    case m => messageNotUnderstood(m)
  }

  private def handleRegistration(): Unit = {
    context.watch(sender)
    /*idleWorkers.append(sender)

    if (idleWorkers.size >= expectedWorkers.values.sum) {
     for (worker <- idleWorkers) {
        assignWorker(worker)
      }
    }*/
    // this might be a bit raceconditiony
    sender().tell(dataSet, self)
    workerPool.workerConnected(sender())
    if (workerPool.isReadyToStart) startWork()
    log.info("Registered {}", sender)
  }

  def handleWelcome(m: WelcomeMessage): Unit = {
    workerPool.slaveConnected(m.workerCount)
    if (workerPool.isReadyToStart) startWork()
    println("local w count ", localWorkerCount, "slave c", slaveNodeCount, "csv ", dataSet, m)
  }

  private def startWork(): Unit = {
    val jobMessages = PasswordJob.splitBetween(workerPool.numberOfIdleWorkers)
    println("jetzt gehts looos")
    for((worker, message ) <- workerPool.idleWorkers zip jobMessages){
      println(message)
      worker.tell(message, self)
    }

  }

  private def handleTermination(message: Terminated): Unit = {
    context.unwatch(message.getActor)
    /*
    if (!idleWorkers.contains(message.getActor)) {
      val work = busyWorkers.remove(message.getActor)
      if (work != null) assignWork(work)
    } else {
      idleWorkers -= message.getActor
    }*/
    log.info("Unregistered {}", message.getActor)
  }

  /*  private def assignWork(work: WorkMessage): Unit = {
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

*/
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



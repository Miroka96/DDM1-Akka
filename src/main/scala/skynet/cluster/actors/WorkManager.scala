package skynet.cluster.actors

import akka.actor.{Actor, ActorRef, ActorSelection, Props, Terminated}
import akka.cluster.Member
import skynet.cluster.SkynetMaster
import skynet.cluster.actors.Messages.{ExerciseJobData, PasswordCrackingResult}
import skynet.cluster.actors.WorkManager._
import skynet.cluster.actors.jobs.PasswordJob
import skynet.cluster.actors.util.ErrorHandling
import skynet.cluster.util.WorkerPool

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

  case class SystemWelcomeMessage(systemIdentifier: String, workerCount: Int)

  case class CSVPerson(id: Int, name: String, passwordhash: String, gene: String)

  case class ResultMessage()


}


class WorkManager(val localWorkerCount: Int,
                  val slaveNodeCount: Int,
                  val dataSet: Array[CSVPerson])
  extends Actor with ErrorHandling {


  /////////////////
  // Actor State //
  /////////////////

  private val workerPool = new WorkerPool(slaveNodeCount, localWorkerCount)

  // Actor Behavior //
  override def receive: Receive = {
    case _: RegistrationMessage => handleRegistration()
    case m: SystemWelcomeMessage => handleWelcome(m)
    case m: Terminated => handleTermination(m)
    case m: PasswordCrackingResult => handlePasswordCrackingResult(m)
    case m => messageNotUnderstood(m)
  }

  private def handleRegistration(): Unit = {
    context.watch(sender)

    sender().tell(ExerciseJobData(dataSet), self)

    workerPool.workerConnected(sender())

    if (workerPool.isReadyToStart) {
      startWork()
    }
  }

  def handleWelcome(m: SystemWelcomeMessage): Unit = {
    workerPool.slaveConnected(m.workerCount)
    if (workerPool.isReadyToStart) startWork()
  }

  private def startWork(): Unit = {
    val jobMessages = PasswordJob.splitIntoNMessages(workerPool.numberOfIdleWorkers)
    println("starting work ")

    val messageIter: Iterator[Messages.PasswordCrackingMessage] = jobMessages.toIterator
    val workerIter: Iterator[ActorRef] = workerPool.idleWorkerClaimer

    while (messageIter.hasNext && workerIter.hasNext) {
      val message = messageIter.next()
      val worker = workerIter.next()
      println(s"handing out work $message to $worker")
      worker.tell(message, self)
    }
  }

  private def handlePasswordCrackingResult(m: PasswordCrackingResult): Unit = {
    println(s"got $m")
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



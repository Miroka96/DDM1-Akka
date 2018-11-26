package skynet.cluster.actors

import akka.actor.{Actor, ActorRef, ActorSelection, Props, Terminated}
import akka.cluster.Member
import skynet.cluster.SkynetMaster
import skynet.cluster.actors.Messages._
import skynet.cluster.actors.WorkManager._
import skynet.cluster.actors.jobs.{LinearCombinationJob, PasswordJob, SubSequenceJob}
import skynet.cluster.actors.util.ErrorHandling
import skynet.cluster.util.WorkerPool

import scala.collection.mutable

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

  case class CrackedPerson(person: CSVPerson,
                           var password: Int = -1,
                           var partner: Int = -1,
                           var prefix: Int = 0,
                           var nonce: Int = -1,
                           var hash: String = null)

}


class WorkManager(val localWorkerCount: Int,
                  val slaveNodeCount: Int,
                  val dataSet: Array[CSVPerson])
  extends Actor with ErrorHandling {


  /////////////////
  // Actor State //
  /////////////////

  private val workerPool = new WorkerPool(slaveNodeCount, localWorkerCount)
  private val unassignedWork = mutable.Queue[JobMessage]()

  private val exerciseResult = dataSet.map(person => (person.id, CrackedPerson(person))).toMap
  private var passwordCrackingFinished = false



  // Actor Behavior //
  override def receive: Receive = {
    case _: RegistrationMessage => handleRegistration()
    case m: SystemWelcomeMessage => handleWelcome(m)
    case m: Terminated => handleTermination(m)
    case m: PasswordCrackingResult => handlePasswordCrackingResult(m)
    case m: LinearCombinationResult => handleLinearCombinationResult(m)
    case m: SubSequenceResult => handleSubsequenceResult(m)
    case m => messageNotUnderstood(m)
  }

  private def handleRegistration(): Unit = {
    context.watch(sender)

    sender().tell(ExerciseJobData(dataSet), self)

    workerPool.workerConnected(sender())

    if (workerPool.isReadyToStart) {
      startPasswordWork()
    }
  }

  def handleWelcome(m: SystemWelcomeMessage): Unit = {
    workerPool.slaveConnected(m.workerCount)
    if (workerPool.isReadyToStart) startPasswordWork()
  }

  private def startPasswordWork(): Unit = {
    log.info("starting work ")
    val jobMessages = PasswordJob.splitIntoNMessages(workerPool.numberOfIdleWorkers * 3)
    jobMessages.foreach(unassignedWork.enqueue(_))

    assignAvailableWork()
  }

  private def assignAvailableWork(): Int = {
    val workerIter: Iterator[ActorRef] = workerPool.idleWorkerClaimer

    var assignmentCount = 0
    while (unassignedWork.nonEmpty && workerIter.hasNext) {
      val message = unassignedWork.dequeue()
      val worker = workerIter.next()
      log.info(s"handing out work $message to $worker")
      worker.tell(message, self)
      assignmentCount += 1
    }
    assignmentCount
  }

  private def handlePasswordCrackingResult(m: PasswordCrackingResult): Unit = {
    log.info(s"got $m from $sender")
    workerPool.freeWorker(sender)
    if (passwordCrackingFinished) return

    m.result.foreach { case (id: Int, password: Int) => exerciseResult(id).password = password }

    assignAvailableWork()

    if (!exerciseResult.exists { case (_, person) => person.password.equals(-1) }) {
      passwordCrackingFinished = true
      log.info("Passwords cracked")
      val resultList = exerciseResult
        .toList
        .sortBy { case (id, _) => id }

      val passwordList = resultList.map(_._2.password).toArray
      resultList.foreach { case (id, person) => log.info(s"$id: ${person.password}") }

      startLinearCombination()
    }
  }

  def handleLinearCombinationResult(m: LinearCombinationResult): Unit = {
    println("got linear combo result")
    val idToPrefix = m.idToPrefix
    for((key,value) <- idToPrefix){
      exerciseResult(key).prefix = value
    }
    workerPool.freeWorker(sender())
    assignAvailableWork()
  }

  def handleSubsequenceResult(m: SubSequenceResult): Unit = {
    println(s"got subsequens res $m")
    exerciseResult(m.id).partner = m.partnerId
    workerPool.freeWorker(sender())
    assignAvailableWork()
    if(unassignedWork.isEmpty){
      println(exerciseResult)
    }
  }


  private def startLinearCombination(): Unit ={
    val idToPassword = exerciseResult.mapValues(w => w.password)
    val messages = LinearCombinationJob.splitIntoNMessages(workerPool.numberOfIdleWorkers * 3, idToPassword)
    messages.foreach(unassignedWork.enqueue(_))
    assignAvailableWork()
    startSubSequenceMatching()
  }

  private def startSubSequenceMatching(): Unit = {
    SubSequenceJob.splitIntoNMessages(workerPool.numberOfIdleWorkers, exerciseResult.keys.size).foreach(
      unassignedWork.enqueue(_)
    )
    assignAvailableWork()
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



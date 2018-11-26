package skynet.cluster.actors

import akka.actor.{Actor, ActorRef, ActorSelection, PoisonPill, Props, Terminated}
import akka.cluster.Member
import skynet.cluster.SkynetMaster
import skynet.cluster.actors.Messages._
import skynet.cluster.actors.WorkManager._
import skynet.cluster.actors.jobs.{HashMiningJob, LinearCombinationJob, PasswordJob, SubSequenceJob}
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
  extends AbstractWorker with ErrorHandling {


  /////////////////
  // Actor State //
  /////////////////

  private val workerPool = new WorkerPool(slaveNodeCount, localWorkerCount)
  private val unassignedWork = mutable.Queue[JobMessage]()

  private val exerciseResult = dataSet.map(person => (person.id, CrackedPerson(person))).toMap
  private var passwordCrackingFinished = false
  private var got0Nonce = false
  private var got1Nonce = false
  private var hashMiningStarted = false
  private var startTime:Long = 0L


  // Actor Behavior //
  override def receive: Receive = {
    case _: RegistrationMessage => handleRegistration()
    case m: SystemWelcomeMessage => handleWelcome(m)
    case m: Terminated => handleTermination(m)
    case m: PasswordCrackingResult => handlePasswordCrackingResult(m)
    case m: LinearCombinationResult => handleLinearCombinationResult(m)
    case m: SubSequenceResult => handleSubsequenceResult(m)
    case m: HashMiningResult => handleHashMiningResult(m)
    case m => messageNotUnderstood(m)
  }

  private def handleRegistration(): Unit = {
    context.watch(sender)

    sender().tell(ExerciseJobData(dataSet), self)

    workerPool.workerConnected(sender())

    if (workerPool.isReadyToStart) {
      startTime = System.currentTimeMillis
      startPasswordWork()
    }
  }

  def handleWelcome(m: SystemWelcomeMessage): Unit = {
    workerPool.slaveConnected(m.workerCount)
    if (workerPool.isReadyToStart) startPasswordWork()
  }

  private def startPasswordWork(): Unit = {
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
      worker.tell(message, self)
      assignmentCount += 1
    }
    assignmentCount
  }

  private def handlePasswordCrackingResult(m: PasswordCrackingResult): Unit = {
    workerPool.freeWorker(sender)
    if (passwordCrackingFinished) return

    m.result.foreach { case (id: Int, password: Int) => exerciseResult(id).password = password }

    assignAvailableWork()

    if (!passwordCrackingFinished && !exerciseResult.exists { case (_, person) => person.password.equals(-1) }) {
      passwordCrackingFinished = true
      startLinearCombination()
    }
  }


  def handleLinearCombinationResult(m: LinearCombinationResult): Unit = {
    val idToPrefix = m.idToPrefix
    for ((key, value) <- idToPrefix) {
      exerciseResult(key).prefix = value
    }
    workerPool.freeWorker(sender())
    assignAvailableWork()
    if (!exerciseResult.exists { case (_, person) => person.partner == -1 || person.prefix == 0 }) {
      startHashMining()
    }
  }


  def handleSubsequenceResult(m: SubSequenceResult): Unit = {
    exerciseResult(m.id).partner = m.partnerId
    workerPool.freeWorker(sender())
    assignAvailableWork()
    if (!exerciseResult.exists { case (_, person) => person.partner == -1 || person.prefix == 0 }) {
      startHashMining()
    }
  }

  def handleHashMiningResult(m: HashMiningResult): Unit = {
    if (m.success && !(got0Nonce && got1Nonce)) {
      if (m.prefix == -1 ) got0Nonce = true else got1Nonce = true
      exerciseResult.foreach { case (id, cracked) => if (m.prefix == cracked.prefix) cracked.hash = m.hash }
    } else if (!(got0Nonce && got1Nonce) && !m.success) {
      unassignedWork.enqueue(HashMiningJob.giveNextMessage(m.job))
    } else {
      println("ID,Name,Password,Prefix,Partner,Hash")
      for(key <- exerciseResult.keys.toList.sorted){
        val person = exerciseResult(key)
        printf("%d;%s;%d;%d;%d;%s\n",key,person.person.name,person.password,person.prefix,person.partner,person.hash)
      }
      printf("Time required: %d ms", System.currentTimeMillis() - startTime)
      workerPool.workerPool.foreach(worker => worker ! PoisonPill)
    }
    workerPool.freeWorker(sender())
    assignAvailableWork()
  }

  private def startLinearCombination(): Unit = {
    val idToPassword = exerciseResult.mapValues(w => w.password)
    unassignedWork ++= LinearCombinationJob.splitIntoNMessages(workerPool.numberOfIdleWorkers * 3, idToPassword)
    assignAvailableWork()
    startSubSequenceMatching()
  }

  private def startSubSequenceMatching(): Unit = {
    unassignedWork ++= SubSequenceJob.splitIntoNMessages(workerPool.numberOfIdleWorkers, exerciseResult.keys.size)
    assignAvailableWork()
  }

  def startHashMining(): Unit = {
    if(hashMiningStarted){assignAvailableWork()}
    else{
      hashMiningStarted = true
      unassignedWork ++= HashMiningJob.splitIntoNMessages(exerciseResult.keys.size, workerPool.numberOfIdleWorkers)
      assignAvailableWork()}
  }


  private def handleTermination(message: Terminated): Unit = {
    context.unwatch(message.getActor)
    if(workerPool.workerPool.size > 1){
      workerPool.removeWorker(message.getActor)
      log.info("Unregistered {}", message.getActor)
    } else {
      workerPool.removeWorker(message.getActor)
      context.stop(self)
    }

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



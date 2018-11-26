package skynet.cluster.actors

import akka.actor.{PoisonPill, Props}
import akka.cluster.ClusterEvent
import skynet.cluster.actors.Messages._
import skynet.cluster.actors.WorkManager.CSVPerson
import skynet.cluster.actors.tasks.{DPLinearCombination, GeneMatching, HashMining, PasswordCracking}
import skynet.cluster.actors.util.{ErrorHandling, RegistrationHandling}

import scala.concurrent.Future

object Messages {

  abstract class JobData()

  abstract class JobMessage()

  abstract class JobResult(var originalJob: JobMessage)

  case class ExerciseJobData(data: Array[CSVPerson]) extends JobData

  case class PasswordCrackingMessage(from: Int, to: Int) extends JobMessage

  case class PasswordCrackingResult(job: PasswordCrackingMessage, result: Map[Int, Int]) extends JobResult(job)

  case class LinearCombinationMessage(idToPassword: Map[Int, Int]) extends JobMessage

  case class LinearCombinationResult(job: LinearCombinationMessage, idToPrefix: Map[Int, Int]) extends JobResult(job)

  case class SubSequenceMessage(id: Int) extends JobMessage

  case class SubSequenceResult(job: SubSequenceMessage, id: Int, partnerId: Int) extends JobResult(job)

  case class HashMiningMessage(maxPartnerNr: Int, from: Int, to: Int) extends JobMessage

  case class HashMiningResult(job: HashMiningMessage, hash: String, prefix: Int, success: Boolean) extends JobResult(job)

}

object Worker {
  ////////////////////////
  // Actor Construction //
  ////////////////////////
  val DEFAULT_NAME = "worker"

  def props: Props = Props.create(classOf[Worker])

}

class Worker extends AbstractWorker with RegistrationHandling with PasswordCracking with DPLinearCombination
  with GeneMatching with HashMining with ErrorHandling {

  import akka.pattern.pipe
  import context.dispatcher

  var dataSet: Array[CSVPerson] = _
  var waitingWork: PasswordCrackingMessage = _


  // Actor Behavior //
  override def receive: Receive = {
    case m: ClusterEvent.CurrentClusterState => handleClusterState(m)
    case m: ClusterEvent.MemberUp => handleMemberUp(m)
    case m: PasswordCrackingMessage => handlePasswordCrackingMessage(m)
    case m: LinearCombinationMessage => handleLinearCombination(m)
    case m: SubSequenceMessage => handleSubSequence(m)
    case m: HashMiningMessage => handleHashMining(m)
    case m: ExerciseJobData => {
      dataSet = m.data
      if (waitingWork != null) {
        val work = waitingWork
        waitingWork = null
        handlePasswordCrackingMessage(waitingWork)
      }
    }
    case PoisonPill => context.stop(self)
    case m => messageNotUnderstood(m)
  }

  def handleHashMining(m: HashMiningMessage): Unit = {
    Future({
      val result = this.mine(m.maxPartnerNr, m.from, m.to)
      if (result.isEmpty) {
        HashMiningResult(m, result, 0, false)
      }
      else {
        if (result.startsWith("00000")) {
          HashMiningResult(m, result, -1, true)
        } else {
          HashMiningResult(m, result, 1, true)
        }
      }
    }).pipeTo(sender())
  }


  def handleSubSequence(m: SubSequenceMessage): Unit = {
    Future({
      val gene = dataSet.find(_.id == m.id).get.gene
      val longest: Int = findPartner(dataSet, m.id, gene)
      SubSequenceResult(m, m.id, longest)
    }).pipeTo(sender())
  }


  private def handlePasswordCrackingMessage(m: PasswordCrackingMessage): Unit = {
    if (dataSet == null) {
      waitingWork = m
      return
    }
    Future({
      val hashesAndIds = dataSet.map(person => (person.passwordhash, person.id)).toMap
      val result = crack(hashesAndIds, m.from, m.to)
      PasswordCrackingResult(m, result)
    }).pipeTo(sender)
  }

  def handleLinearCombination(m: LinearCombinationMessage): Unit = {
    Future({
      val result = this.solveLinearCombination(m.idToPassword)
      LinearCombinationResult(m, result)
    }).pipeTo(sender)
  }

}

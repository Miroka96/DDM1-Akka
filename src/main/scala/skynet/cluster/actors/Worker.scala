package skynet.cluster.actors

import akka.actor.Props
import akka.cluster.ClusterEvent
import skynet.cluster.actors.Messages.{ExerciseJobData, PasswordCrackingMessage, PasswordCrackingResult}
import skynet.cluster.actors.WorkManager.CSVPerson
import skynet.cluster.actors.tasks.PasswordCracking
import skynet.cluster.actors.util.{ErrorHandling, RegistrationHandling}

import scala.concurrent.Future

object Messages {

  abstract class JobData()
  abstract class JobMessage()

  abstract class JobResult(var originalJob: JobMessage)

  case class ExerciseJobData(data: Array[CSVPerson]) extends JobData

  case class PasswordCrackingMessage(from: Int, to: Int) extends JobMessage
  case class PasswordCrackingResult(job: PasswordCrackingMessage, result: Map[Int, Int]) extends JobResult(job)

  case class PasswordData(passwords: Array[Int]) extends JobData

}

object Worker {
  ////////////////////////
  // Actor Construction //
  ////////////////////////
  val DEFAULT_NAME = "worker"

  def props: Props = Props.create(classOf[Worker])

}

class Worker extends AbstractWorker with RegistrationHandling with PasswordCracking with ErrorHandling {

  import akka.pattern.pipe
  import context.dispatcher

  var dataSet: Array[CSVPerson] = _
  var waitingWork: PasswordCrackingMessage = _

  // Actor Behavior //
  override def receive: Receive = {
    case m: ClusterEvent.CurrentClusterState => handleClusterState(m)
    case m: ClusterEvent.MemberUp => handleMemberUp(m)
    case m: PasswordCrackingMessage => handlePasswordCrackingMessage(m)
    case m: ExerciseJobData => {
      dataSet = m.data
      if (waitingWork != null) {
        val work = waitingWork
        waitingWork = null
        handlePasswordCrackingMessage(waitingWork)
      }
    }
    case m => messageNotUnderstood(m)
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

  /*private def handleWork(message: WorkMessage): Unit = {
    val result = message.runOn(this)
    sender.tell(result, self)
  }*/

  override protected def masterFound(): Unit = {
    workManager.tell("lolololol", self)
  }
}

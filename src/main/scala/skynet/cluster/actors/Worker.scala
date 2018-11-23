package skynet.cluster.actors

import akka.actor.Props
import akka.cluster.ClusterEvent
import skynet.cluster.actors.Messages.{PasswordCrackingMessage, PasswordCrackingResult}
import skynet.cluster.actors.WorkManager.CSVPerson
import skynet.cluster.actors.tasks.PasswordCracking
import skynet.cluster.actors.util.{ErrorHandling, RegistrationHandling}

import scala.concurrent.Future

object Messages {

  abstract class JobMessage()

  abstract class JobResult(var originalJob: JobMessage)
  case class PasswordCrackingMessage(from: Int, to: Int) extends JobMessage

  case class PasswordCrackingResult(job: PasswordCrackingMessage, result: Map[Int, String]) extends JobResult(job)

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

  // Actor Behavior //
  override def receive: Receive = {
    case m: ClusterEvent.CurrentClusterState => handleClusterState(m)
    case m: ClusterEvent.MemberUp => handleMemberUp(m)
    case m: PasswordCrackingMessage => handlePasswordCrackingMessage(m)
    case m: Array[CSVPerson] => dataSet = m
    case m => messageNotUnderstood(m)
  }

  private def handlePasswordCrackingMessage(m: PasswordCrackingMessage): Unit = {
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

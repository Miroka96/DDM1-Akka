package skynet.cluster.actors

import akka.actor.Props
import akka.cluster.ClusterEvent
import skynet.cluster.actors.Messages.PasswordCrackingMessage
import skynet.cluster.actors.WorkManager.CSVPerson
import skynet.cluster.actors.tasks.PasswordCracking
import skynet.cluster.actors.util.{ErrorHandling, RegistrationHandling}

object Messages {

  abstract class JobMessage()
  case class PasswordCrackingMessage(from: Int, to: Int) extends JobMessage


}

object Worker {
  ////////////////////////
  // Actor Construction //
  ////////////////////////
  val DEFAULT_NAME = "worker"

  def props: Props = Props.create(classOf[Worker])

}

class Worker extends AbstractWorker with RegistrationHandling with PasswordCracking with ErrorHandling {
  var dataSet: Array[CSVPerson] = _

  // Actor Behavior //
  override def receive: Receive = {
    case m: ClusterEvent.CurrentClusterState => handleClusterState(m)
    case m: ClusterEvent.MemberUp => handleMemberUp(m)
    case m: PasswordCrackingMessage => crack(dataSet.map(person => (person.passwordhash, person.id)).toMap, m.from, m.to)
    case m: Array[CSVPerson] => dataSet = m
    case m => messageNotUnderstood(m)
  }

  /*private def handleWork(message: WorkMessage): Unit = {
    val result = message.runOn(this)
    sender.tell(result, self)
  }*/

  override protected def masterFound(): Unit = {
    workManager.tell("lolololol", self)
  }
}

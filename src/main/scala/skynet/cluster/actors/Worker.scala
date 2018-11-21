package skynet.cluster.actors

import akka.actor.Props
import akka.cluster.ClusterEvent
import skynet.cluster.actors.tasks.PasswordCracking
import skynet.cluster.actors.util.{ErrorHandling, RegistrationHandling}

object Messages {

  object PasswordCracking {

    case class WorkMessage()

    case class WorkResult()

  }

}

object Worker {
  ////////////////////////
  // Actor Construction //
  ////////////////////////
  val DEFAULT_NAME = "worker"

  def props: Props = Props.create(classOf[Worker])

}

class Worker extends AbstractWorker
  with RegistrationHandling
  with PasswordCracking
  with ErrorHandling {

  // Actor Behavior //
  override def receive: Receive = {
    case m: ClusterEvent.CurrentClusterState => handleClusterState(m)
    case m: ClusterEvent.MemberUp => handleMemberUp(m)
    case m: WorkMessage => handleWork(m)
    case m => messageNotUnderstood(m)
  }

  private def handleWork(message: WorkMessage): Unit = {
    val result = message.runOn(this)
    sender.tell(result, self)
  }

  override protected def masterFound(): Unit = {
    workManager.tell("lolololol", self)
  }
}

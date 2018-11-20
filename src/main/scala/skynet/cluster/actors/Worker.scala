package skynet.cluster.actors

import akka.actor.{AbstractActor, Props}
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
  override def createReceive: AbstractActor.Receive =
    receiveBuilder
      .`match`(classOf[ClusterEvent.CurrentClusterState], handleClusterState)
      .`match`(classOf[ClusterEvent.MemberUp], handleMemberUp)
      .`match`(classOf[WorkMessage], handleWork)
      .matchAny(messageNotUnderstood)
      .build

  private def handleWork(message: WorkMessage): Unit = {
    val result = message.runOn(this)
    sender.tell(result, self)
  }

}

package skynet.cluster.actors.listeners

import akka.actor.Props
import akka.cluster.ClusterEvent
import skynet.cluster.actors.AbstractWorker
import skynet.cluster.actors.util.ErrorHandling


object ClusterListener {
  ////////////////////////
  // Actor Construction //
  ////////////////////////

  val DEFAULT_NAME = "clusterListener"

  def props: Props = Props.create(classOf[ClusterListener])
}

class ClusterListener extends AbstractWorker with ErrorHandling {

  /////////////////////
  // Actor Lifecycle //
  /////////////////////
  override def preStart(): Unit = {
    cluster.subscribe(self, classOf[ClusterEvent.MemberEvent], classOf[ClusterEvent.UnreachableMember])
  }


  ////////////////////
  // Actor Behavior //
  ////////////////////
  override def receive: Receive = {
    case state: ClusterEvent.CurrentClusterState =>
      log.info("Current members: {}", state.members)
    case mUp: ClusterEvent.MemberUp =>
      log.info("Member is Up: {}", mUp.member)
    case mUnreachable: ClusterEvent.UnreachableMember =>
      log.info("Member detected as unreachable: {}", mUnreachable.member)
    case mRemoved: ClusterEvent.MemberRemoved =>
      log.info("Member is Removed: {}", mRemoved.member)
    case _: ClusterEvent.MemberEvent => ignoreMessage
  }
}

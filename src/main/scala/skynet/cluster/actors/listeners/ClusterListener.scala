package skynet.cluster.actors.listeners

import akka.actor.{AbstractActor, Props}
import akka.cluster.{Cluster, ClusterEvent}
import akka.event.{Logging, LoggingAdapter}


object ClusterListener {
  ////////////////////////
  // Actor Construction //
  ////////////////////////

  val DEFAULT_NAME = "clusterListener"

  def props: Props = Props.create(classOf[ClusterListener])
}

class ClusterListener extends AbstractActor {
  /////////////////
  // Actor State //
  /////////////////
  final private val log: LoggingAdapter = Logging.getLogger(context.system, this)
  final private val cluster = Cluster.get(context.system)

  /////////////////////
  // Actor Lifecycle //
  /////////////////////
  override def preStart(): Unit = {
    cluster.subscribe(self, classOf[ClusterEvent.MemberEvent], classOf[ClusterEvent.UnreachableMember])
  }

  override def postStop(): Unit = {
    cluster.unsubscribe(self)
  }

  ////////////////////
  // Actor Behavior //
  ////////////////////
  override def createReceive: AbstractActor.Receive =
    receiveBuilder.`match`(classOf[ClusterEvent.CurrentClusterState], (state: ClusterEvent.CurrentClusterState) => {
      log.info("Current members: {}", state.members)
    }).`match`(classOf[ClusterEvent.MemberUp], (mUp: ClusterEvent.MemberUp) => {
      log.info("Member is Up: {}", mUp.member)
    }).`match`(classOf[ClusterEvent.UnreachableMember], (mUnreachable: ClusterEvent.UnreachableMember) => {
      log.info("Member detected as unreachable: {}", mUnreachable.member)
    }).`match`(classOf[ClusterEvent.MemberRemoved], (mRemoved: ClusterEvent.MemberRemoved) => {
      log.info("Member is Removed: {}", mRemoved.member)
    }).`match`(classOf[ClusterEvent.MemberEvent], (message: ClusterEvent.MemberEvent) => {
      // ignore
    }).build
}

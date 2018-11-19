package skynet.cluster.actors

import akka.actor.{AbstractActor, Props}
import akka.cluster.{Cluster, ClusterEvent, Member, MemberStatus}
import akka.event.Logging
import skynet.cluster.SkynetMaster


object Worker {
  ////////////////////////
  // Actor Construction //
  ////////////////////////
  val DEFAULT_NAME = "worker"

  def props: Props = Props.create(classOf[Worker])

  ////////////////////
  // Actor Messages //
  ////////////////////
  @SerialVersionUID(-7643194361868862395L)
  case class WorkMessage(x: Array[Int], y: Array[Int])

}

class Worker extends AbstractActor {
  /////////////////
  // Actor State //
  /////////////////
  final private val log = Logging.getLogger(this.context.system, this)
  final private val cluster = Cluster.get(this.context.system)

  /////////////////////
  // Actor Lifecycle //
  /////////////////////
  override def preStart(): Unit = {
    this.cluster.subscribe(this.self, classOf[ClusterEvent.MemberUp])
  }

  override def postStop(): Unit = {
    this.cluster.unsubscribe(this.self)
  }

  // Actor Behavior //
  override def createReceive: AbstractActor.Receive =
    receiveBuilder
      .`match`(classOf[ClusterEvent.CurrentClusterState], handleClusterState)
      .`match`(classOf[ClusterEvent.MemberUp], handleMemberUp)
      .`match`(classOf[Worker.WorkMessage], handleWork)
      .matchAny((`object`: Any) =>
        this.log.info("Received unknown message: \"{}\"", `object`.toString))
      .build

  private def handleClusterState(message: ClusterEvent.CurrentClusterState): Unit = {
    message.getMembers.forEach((member: Member) => {
      if (member.status == MemberStatus.up) this.register(member)
    })
  }

  private def handleMemberUp(message: ClusterEvent.MemberUp): Unit = {
    this.register(message.member)
  }

  private def register(member: Member): Unit = {
    if (member.hasRole(SkynetMaster.MASTER_ROLE))
      getContext.actorSelection(member.address + "/user/" + Profiler.DEFAULT_NAME)
        .tell(new Profiler.RegistrationMessage, self)
  }

  private def handleWork(message: Worker.WorkMessage): Unit = {
    var y = 0
    for (i <- 0 until 1000000) {
      if (isPrime(i)) y = y + i
    }
    this.log.info("done: " + y)
    this.sender.tell(new Profiler.CompletionMessage(Profiler.CompletionMessage.CompletionStatus.EXTENDABLE), this.self)
  }

  private def isPrime(n: Long): Boolean = { // Check for the most basic primes
    if (n == 1 || n == 2 || n == 3) return true
    // Check if n is an even number
    if (n % 2 == 0) return false
    // Check the odds
    var i = 3
    while (i * i <= n) {
      if (n % i == 0) return false
      i += 2
    }
    true
  }
}

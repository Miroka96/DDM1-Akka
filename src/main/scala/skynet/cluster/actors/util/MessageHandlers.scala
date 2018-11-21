package skynet.cluster.actors.util

import akka.cluster.{ClusterEvent, Member, MemberStatus}
import skynet.cluster.actors.{Logging, RegistrationProcess}

trait ErrorHandling extends Logging {
  protected def messageNotUnderstood(o: Any): Unit = {
    log.info("Received unknown message: \"{}\"", o.toString)
  }

  protected def ignoreMessage: Unit = {}

}

trait RegistrationHandling extends RegistrationProcess {
  protected def handleClusterState(message: ClusterEvent.CurrentClusterState): Unit = {
    message.getMembers.forEach((member: Member) => {
      if (member.status == MemberStatus.up) eventuallyRegister(member)
    })
  }

  protected def handleMemberUp(message: ClusterEvent.MemberUp): Unit = eventuallyRegister(message.member)
}

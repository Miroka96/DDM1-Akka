package skynet.cluster.actors

import akka.actor.Actor
import akka.cluster.{Cluster, ClusterEvent}
import akka.event.{Logging, LoggingAdapter}

trait Logging extends Actor {
  final protected val log: LoggingAdapter = Logging.getLogger(context.system, this)
}

abstract class AbstractWorker extends Actor with Logging {

  final protected val cluster = Cluster.get(context.system)

  override def preStart(): Unit = cluster.subscribe(self, classOf[ClusterEvent.MemberUp])

  override def postStop(): Unit = cluster.unsubscribe(self)
}

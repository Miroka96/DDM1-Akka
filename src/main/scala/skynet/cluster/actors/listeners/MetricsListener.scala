package skynet.cluster.actors.listeners

import akka.actor.{AbstractActor, Props}
import akka.cluster.metrics.{ClusterMetricsChanged, ClusterMetricsExtension, NodeMetrics, StandardMetrics}
import akka.cluster.{Cluster, ClusterEvent}
import akka.event.Logging


object MetricsListener {
  ////////////////////////
  // Actor Construction //
  ////////////////////////
  val DEFAULT_NAME = "metricsListener"

  def props: Props = Props.create(classOf[MetricsListener])
}

class MetricsListener extends AbstractActor {
  /////////////////
  // Actor State //
  /////////////////
  final private val log = Logging.getLogger(getContext.system, this)
  final private val cluster = Cluster.get(getContext.system)
  final private val extension = ClusterMetricsExtension.get(getContext.system)

  /////////////////////
  // Actor Lifecycle //
  /////////////////////
  override def preStart(): Unit = {
    this.extension.subscribe(self)
  }

  override def postStop(): Unit = {
    this.extension.unsubscribe(self)
  }

  ////////////////////
  // Actor Behavior //
  ////////////////////
  override def createReceive: AbstractActor.Receive =
    receiveBuilder
      .`match`(classOf[ClusterMetricsChanged], logMetrics)
      .`match`(classOf[ClusterEvent.CurrentClusterState], (message: ClusterEvent.CurrentClusterState) => {})
      .build

  private def logMetrics(clusterMetrics: ClusterMetricsChanged): Unit = {
    import scala.collection.JavaConversions._
    for (nodeMetrics <- clusterMetrics.getNodeMetrics) {
      if (nodeMetrics.address == cluster.selfAddress) {
        logHeap(nodeMetrics)
        logCpu(nodeMetrics)
      }
    }
  }

  private def logHeap(nodeMetrics: NodeMetrics): Unit = {
    val heap = StandardMetrics.extractHeapMemory(nodeMetrics)
    if (heap != null) log.info("Used heap: {} MB", heap.used.toDouble / 1024 / 1024)
  }

  private def logCpu(nodeMetrics: NodeMetrics): Unit = {
    val cpu = StandardMetrics.extractCpu(nodeMetrics)
    if (cpu != null && cpu.systemLoadAverage.isDefined)
      log.info("Load: {} ({} processors)", cpu.systemLoadAverage.get, cpu.processors)
  }
}

package skynet.cluster.actors.listeners

import akka.actor.{Actor, Props}
import akka.cluster.metrics.{ClusterMetricsChanged, ClusterMetricsExtension, NodeMetrics, StandardMetrics}
import akka.cluster.{Cluster, ClusterEvent}
import akka.event.Logging
import skynet.cluster.actors.util.ErrorHandling


object MetricsListener {
  ////////////////////////
  // Actor Construction //
  ////////////////////////
  val DEFAULT_NAME = "metricsListener"

  def props: Props = Props.create(classOf[MetricsListener])
}

class MetricsListener extends Actor with ErrorHandling {
  /////////////////
  // Actor State //
  /////////////////
  final private val log = Logging.getLogger(context.system, this)
  final private val cluster = Cluster.get(context.system)
  final private val extension = ClusterMetricsExtension.get(context.system)

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
  override def receive: Receive = {
    case m: ClusterMetricsChanged => logMetrics(m)
    case _: ClusterEvent.CurrentClusterState => ignoreMessage
    case m => messageNotUnderstood(m)
  }

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

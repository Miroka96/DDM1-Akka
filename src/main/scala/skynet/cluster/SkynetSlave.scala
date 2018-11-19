package skynet.cluster

import akka.cluster.Cluster
import skynet.cluster.actors.Worker
import skynet.cluster.actors.listeners.{ClusterListener, MetricsListener}


object SkynetSlave extends SkynetSystem {
  val SLAVE_ROLE = "slave"

  def start(actorSystemName: String,
            workers: Int,
            host: String,
            port: Int,
            masterhost: String,
            masterport: Int
           ): Unit = {
    val config = createConfiguration(actorSystemName, SLAVE_ROLE, host, port, masterhost, masterport)
    val system = createSystem(actorSystemName, config)

    Cluster.get(system).registerOnMemberUp(() => {
      system.actorOf(ClusterListener.props, ClusterListener.DEFAULT_NAME)
      system.actorOf(MetricsListener.props, MetricsListener.DEFAULT_NAME)

      for (i <- 0 until workers) {
        system.actorOf(Worker.props, Worker.DEFAULT_NAME + i)
      }
    })
  }
}

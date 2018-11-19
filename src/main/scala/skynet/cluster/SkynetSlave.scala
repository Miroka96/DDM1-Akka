package skynet.cluster

import akka.cluster.Cluster
import skynet.cluster.actors.Worker
import skynet.cluster.actors.listeners.MetricsListener


object SkynetSlave extends SkynetSystem {
  val SLAVE_ROLE = "slave"

  def start(actorSystemName: String, workers: Int, host: String, port: Int, masterhost: String, masterport: Int): Unit = {
    val config = createConfiguration(actorSystemName, SLAVE_ROLE, host, port, masterhost, masterport)
    val system = createSystem(actorSystemName, config)
    Cluster.get(system).registerOnMemberUp(new Runnable() {
      override def run(): Unit = { //system.actorOf(ClusterListener.props(), ClusterListener.DEFAULT_NAME);
        system.actorOf(MetricsListener.props, MetricsListener.DEFAULT_NAME)
        var i = 0
        while ( {
          i < workers
        }) system.actorOf(Worker.props, Worker.DEFAULT_NAME + i) {
          i += 1;
          i - 1
        }
      }
    })
  }
}

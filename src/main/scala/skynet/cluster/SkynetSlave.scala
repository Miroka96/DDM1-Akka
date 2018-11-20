package skynet.cluster

import akka.cluster.Cluster


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
      spawnBackbone(system, workers)
    })
  }
}

package skynet.cluster

import akka.actor.ActorRef
import akka.cluster.Cluster
import skynet.cluster.actors.WorkManager
import skynet.cluster.actors.WorkManager.SystemWelcomeMessage


object SkynetSlave extends SkynetSystem {
  val SLAVE_ROLE = "slave"

  def start(actorSystemName: String,
            workerCount: Int,
            host: String,
            port: Int,
            masterhost: String,
            masterport: Int
           ): Unit = {
    val config = createConfiguration(actorSystemName, SLAVE_ROLE, host, port, masterhost, masterport)
    val system = createSystem(actorSystemName, config)

    Cluster.get(system).registerOnMemberUp(() => {
      spawnBackbone(system, workerCount)
    })

    for (seednode <- Cluster.get(system).settings.SeedNodes) {
      println(seednode)
      system.actorSelection(seednode + "/user/" + WorkManager.DEFAULT_NAME)
        .tell(SystemWelcomeMessage("%s:%s".format(host, port), workerCount), ActorRef.noSender)
    }

  }
}

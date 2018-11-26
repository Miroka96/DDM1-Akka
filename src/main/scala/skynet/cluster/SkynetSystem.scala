package skynet.cluster

import java.util.concurrent.TimeUnit

import akka.actor.ActorSystem
import akka.cluster.Cluster
import com.typesafe.config.{Config, ConfigFactory}
import skynet.cluster.actors.WorkManager.CSVPerson
import skynet.cluster.actors.Worker
import skynet.cluster.actors.listeners.ClusterListener

import scala.concurrent.Await
import scala.concurrent.duration.Duration


abstract class SkynetSystem {
  // Create the Config with fallback to the application config
  protected def createConfiguration(actorSystemName: String,
                                    actorSystemRole: String,
                                    host: String,
                                    port: Int,
                                    masterhost: String,
                                    masterport: Int
                                   ): Config = {
    ConfigFactory.parseString(
      "akka.remote.netty.tcp.hostname = \"" + host + "\"\n"
        + "akka.remote.netty.tcp.port = " + port + "\n"
        + "akka.remote.artery.canonical.hostname = \"" + host + "\"\n"
        + "akka.remote.artery.canonical.port = " + port + "\n"
        + "akka.cluster.roles = [" + actorSystemRole + "]\n"
        + "akka.cluster.seed-nodes = [\"akka://" + actorSystemName + "@" + masterhost + ":" + masterport + "\"]")
      .withFallback(ConfigFactory.load("skynet"))
  }

  protected def createSystem(actorSystemName: String, config: Config): ActorSystem = {
    val system = ActorSystem.create(actorSystemName, config)

    // Register a callback that ends the program when the ActorSystem terminates
    system.registerOnTermination(() => {
      System.exit(0)
    })

    // Register a callback that terminates the ActorSystem when it is detached from the cluster
    Cluster.get(system).registerOnMemberRemoved(() => {
      system.terminate
      /*new Thread() {
        override def run(): Unit = {
          try
            Await.ready(system.whenTerminated, Duration.create(10, TimeUnit.SECONDS))
          catch {
            case _: Exception =>
              System.exit(-1)
          }
        }
      }.start()*/
    }
    )
    system
  }

  protected final def spawnBackbone(system: ActorSystem, workerCount: Int, slaveNodeCount: Int = 0, dataSet: Array[CSVPerson] = null): Unit = {
    system.actorOf(ClusterListener.props, ClusterListener.DEFAULT_NAME)
    //system.actorOf(MetricsListener.props, MetricsListener.DEFAULT_NAME)

    spawnSpecialBackbone(system, workerCount, slaveNodeCount, dataSet)

    (0 until workerCount).foreach(i =>
      system.actorOf(Worker.props, Worker.DEFAULT_NAME + i)
    )
  }

  protected def spawnSpecialBackbone(system: ActorSystem, workerCount: Int, slaveNodeCount: Int, dataSet: Array[CSVPerson]): Unit = {}

}

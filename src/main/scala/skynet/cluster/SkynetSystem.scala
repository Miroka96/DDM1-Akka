package skynet.cluster

import java.util.concurrent.TimeUnit

import akka.actor.ActorSystem
import akka.cluster.Cluster
import com.typesafe.config.{Config, ConfigFactory}

import scala.concurrent.Await
import scala.concurrent.duration.Duration


trait SkynetSystem {
  protected def createConfiguration(actorSystemName: String,
                                    actorSystemRole: String,
                                    host: String,
                                    port: Int,
                                    masterhost: String,
                                    masterport: Int
                                   ): Config = { // Create the Config with fallback to the application config
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
    system.registerOnTermination(new Runnable() {
      override def run(): Unit = {
        System.exit(0)
      }
    })

    // Register a callback that terminates the ActorSystem when it is detached from the cluster
    Cluster.get(system).registerOnMemberRemoved(new Runnable() {
      override def run(): Unit = {
        system.terminate
        new Thread() {
          override def run(): Unit = {
            try
              Await.ready(system.whenTerminated, Duration.create(10, TimeUnit.SECONDS))
            catch {
              case e: Exception =>
                System.exit(-1)
            }
          }
        }.start()
      }
    })
    system
  }
}

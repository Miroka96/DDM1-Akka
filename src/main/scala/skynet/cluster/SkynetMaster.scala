package skynet.cluster

import java.util.Scanner

import akka.actor.ActorRef
import akka.cluster.Cluster
import skynet.cluster.actors.listeners.{ClusterListener, MetricsListener}
import skynet.cluster.actors.{WorkManager, Worker}


object SkynetMaster extends SkynetSystem {
  val MASTER_ROLE = "master"

  def start(actorSystemName: String, workers: Int, host: String, port: Int): Unit = {
    val config = createConfiguration(actorSystemName, MASTER_ROLE, host, port, host, port)
    val system = createSystem(actorSystemName, config)

    Cluster.get(system).registerOnMemberUp(() => {
      system.actorOf(ClusterListener.props, ClusterListener.DEFAULT_NAME)
      system.actorOf(MetricsListener.props, MetricsListener.DEFAULT_NAME)

      system.actorOf(WorkManager.props, WorkManager.DEFAULT_NAME)
      for (i <- 0 until workers) {
        system.actorOf(Worker.props, Worker.DEFAULT_NAME + i)
      }

      //	int maxInstancesPerNode = workers; // TODO: Every node gets the same number of workers, so it cannot be a parameter for the slave nodes
      //	Set<String> useRoles = new HashSet<>(Arrays.asList("master", "slave"));
      //	ActorRef router = system.actorOf(
      //		new ClusterRouterPool(
      //			new AdaptiveLoadBalancingPool(SystemLoadAverageMetricsSelector.getInstance(), 0),
      //			new ClusterRouterPoolSettings(10000, workers, true, new HashSet<>(Arrays.asList("master", "slave"))))
      //		.props(Props.create(Worker.class)), "router");
    })

    val scanner = new Scanner(System.in)
    val line = scanner.nextLine
    scanner.close()
    val attributes = line.toInt
    system.actorSelection("/user/" + WorkManager.DEFAULT_NAME).tell(WorkManager.TaskMessage(attributes), ActorRef.noSender)
  }
}

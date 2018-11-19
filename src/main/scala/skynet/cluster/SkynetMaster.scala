package skynet.cluster

import java.util.Scanner

import akka.actor.ActorRef
import akka.cluster.Cluster
import skynet.cluster.actors.{Profiler, Worker}
import skynet.cluster.actors.listeners.ClusterListener


object SkynetMaster extends SkynetSystem {
  val MASTER_ROLE = "master"

  def start(actorSystemName: String, workers: Int, host: String, port: Int): Unit = {
    val config = createConfiguration(actorSystemName, MASTER_ROLE, host, port, host, port)
    val system = createSystem(actorSystemName, config)
    Cluster.get(system).registerOnMemberUp(new Runnable() {
      override def run(): Unit = {
        system.actorOf(ClusterListener.props, ClusterListener.DEFAULT_NAME)
        //	system.actorOf(MetricsListener.props(), MetricsListener.DEFAULT_NAME);
        system.actorOf(Profiler.props, Profiler.DEFAULT_NAME)
        var i = 0
        while ( {
          i < workers
        }) system.actorOf(Worker.props, Worker.DEFAULT_NAME + i) {
          i += 1;
          i - 1
        }
        //	int maxInstancesPerNode = workers; // TODO: Every node gets the same number of workers, so it cannot be a parameter for the slave nodes
        //	Set<String> useRoles = new HashSet<>(Arrays.asList("master", "slave"));
        //	ActorRef router = system.actorOf(
        //		new ClusterRouterPool(
        //			new AdaptiveLoadBalancingPool(SystemLoadAverageMetricsSelector.getInstance(), 0),
        //			new ClusterRouterPoolSettings(10000, workers, true, new HashSet<>(Arrays.asList("master", "slave"))))
        //		.props(Props.create(Worker.class)), "router");
      }
    })
    val scanner = new Scanner(System.in)
    val line = scanner.nextLine
    scanner.close()
    val attributes = line.toInt
    system.actorSelection("/user/" + Profiler.DEFAULT_NAME).tell(new Profiler.TaskMessage(attributes), ActorRef.noSender)
  }
}

package skynet.cluster

import akka.actor.{ActorRef, ActorSystem}
import akka.cluster.Cluster
import skynet.cluster.actors.{TaskMessage, WorkManager}


object SkynetMaster extends SkynetSystem {
  val MASTER_ROLE = "master"

  def start(actorSystemName: String, workers: Int, host: String, port: Int, inputFilename: String): Unit = {
    val config = createConfiguration(actorSystemName, MASTER_ROLE, host, port, host, port)
    val system: ActorSystem = createSystem(actorSystemName, config)

    Cluster.get(system).registerOnMemberUp(() => {
      spawnBackbone(system, workers)

      //	int maxInstancesPerNode = workers; // TODO: Every node gets the same number of workers, so it cannot be a parameter for the slave nodes
      //	Set<String> useRoles = new HashSet<>(Arrays.asList("master", "slave"));
      //	ActorRef router = system.actorOf(
      //		new ClusterRouterPool(
      //			new AdaptiveLoadBalancingPool(SystemLoadAverageMetricsSelector.getInstance(), 0),
      //			new ClusterRouterPoolSettings(10000, workers, true, new HashSet<>(Arrays.asList("master", "slave"))))
      //		.props(Props.create(Worker.class)), "router");
    })

    system.actorSelection("/user/" + WorkManager.DEFAULT_NAME)
      .tell(getInitialTask(inputFilename), ActorRef.noSender)
  }

  protected def getInitialTask(filename: String): TaskMessage = {

    null
  }

  override def spawnSpecialBackbone(system: ActorSystem): Unit = {
    system.actorOf(WorkManager.props, WorkManager.DEFAULT_NAME)
  }
}

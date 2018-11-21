package skynet.cluster

import akka.actor.{ActorRef, ActorSystem}
import akka.cluster.Cluster
import skynet.cluster.actors.tasks.ExerciseTask
import skynet.cluster.actors.tasks.ExerciseTask.CSVPerson
import skynet.cluster.actors.{TaskMessage, WorkManager}

import scala.io.Source


object SkynetMaster extends SkynetSystem {
  val MASTER_ROLE = "master"

  def start(actorSystemName: String, workerCount: Int, host: String, port: Int, inputFilename: String, slaveCount: Int): Unit = {
    val config = createConfiguration(actorSystemName, MASTER_ROLE, host, port, host, port)
    val system: ActorSystem = createSystem(actorSystemName, config)

    Cluster.get(system).registerOnMemberUp(() => {
      spawnBackbone(system, workerCount)

      //	int maxInstancesPerNode = workers; // TODO: Every node gets the same number of workers, so it cannot be a parameter for the slave nodes
      //	Set<String> useRoles = new HashSet<>(Arrays.asList("master", "slave"));
      //	ActorRef router = system.actorOf(
      //		new ClusterRouterPool(
      //			new AdaptiveLoadBalancingPool(SystemLoadAverageMetricsSelector.getInstance(), 0),
      //			new ClusterRouterPoolSettings(10000, workers, true, new HashSet<>(Arrays.asList("master", "slave"))))
      //		.props(Props.create(Worker.class)), "router");
    })

    system.actorSelection("/user/" + WorkManager.DEFAULT_NAME)
      .tell(getInitialTask(inputFilename, slaveCount), ActorRef.noSender)
  }

  protected def getInitialTask(filename: String, slaveCount: Int): TaskMessage = {
    val file = Source.fromFile(filename)
    val persons = file.getLines()
      .drop(1).filterNot(line => line == "")
      .map(line => {
        val parts = line.split(";")
        CSVPerson(parts(0).toInt, parts(1), parts(2), parts(3))
      })
      .toArray
    ExerciseTask(persons, slaveCount)
  }

  override def spawnSpecialBackbone(system: ActorSystem): Unit = {
    system.actorOf(WorkManager.props, WorkManager.DEFAULT_NAME)
  }
}

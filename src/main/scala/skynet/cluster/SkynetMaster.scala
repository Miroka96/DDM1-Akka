package skynet.cluster

import akka.actor.{ActorRef, ActorSystem}
import akka.cluster.Cluster
import skynet.cluster.actors.WorkManager
import skynet.cluster.actors.WorkManager.CSVPerson

import scala.io.Source


object SkynetMaster extends SkynetSystem {
  val MASTER_ROLE = "master"

  def start(actorSystemName: String, workerCount: Int, host: String, port: Int, inputFilename: String, slaveCount: Int): Unit = {
    val config = createConfiguration(actorSystemName, MASTER_ROLE, host, port, host, port)
    val system: ActorSystem = createSystem(actorSystemName, config)

    Cluster.get(system).registerOnMemberUp(() => {
      val file = Source.fromFile(inputFilename)
      val persons = file.getLines()
        .drop(1).filterNot(line => line == "")
        .map(line => {
          val parts = line.split(";")
          CSVPerson(parts(0).toInt, parts(1), parts(2), parts(3))
        })
        .toArray
      spawnBackbone(system, workerCount, slaveCount, persons)
    })

  }


  override def spawnSpecialBackbone(system: ActorSystem, workerCount: Int, slaveNodeCount: Int, dataSet: Array[CSVPerson]): Unit = {
    system.actorOf(WorkManager.props(workerCount, slaveNodeCount, dataSet), WorkManager.DEFAULT_NAME)
  }
}

package skynet.cluster.util

import akka.actor.ActorRef
import skynet.cluster.actors.Messages.PasswordCrackingMessage
import skynet.cluster.actors.jobs.PasswordJob

import scala.collection.mutable.ArrayBuffer

class WorkerPool(private var slaveCount: Int, localWorkers: Int) {
  private var unconnectedSlaves: Int = slaveCount
  private var unconnectedWorkers: Int = localWorkers
  val idleWorkers = new ArrayBuffer[ActorRef]

  def workerConnected(worker: ActorRef): Unit = {
    this.unconnectedWorkers -= 1
    idleWorkers += worker
    if(! isReadyToStart) println("Not all workers connected, missing: ", unconnectedWorkers)
  }

  def slaveConnected(workerCount: Int): Unit = {
    this.unconnectedSlaves -= 1
    this.unconnectedWorkers += workerCount
  }

  def isReadyToStart: Boolean = {
   unconnectedSlaves==0 && unconnectedWorkers == 0
  }

  def numberOfIdleWorkers: Int = {
    idleWorkers.size
  }



}

package skynet.cluster.util

import java.io.PrintStream

import akka.actor.ActorRef

import scala.collection.mutable

class WorkerPool(private var slaveCount: Int, localWorkers: Int) {

  val workerPool: mutable.Set[ActorRef] = mutable.SortedSet[ActorRef]()


  private var expectedSlaves = slaveCount
  private var connectedSlaves = 0
  private var expectedWorkers = localWorkers
  private var connectedWorkers = 0

  def workerConnected(worker: ActorRef): Unit = {
    connectedWorkers += 1
    addWorker(worker)
    if (!isReadyToStart) println("Not everybody connected:")
    printStatus()
  }

  def printStatus(out: PrintStream = System.out): Unit = {
    out.println(s"Slaves: $connectedSlaves/$expectedSlaves\nWorkers: $connectedWorkers/$expectedWorkers")
  }

  def addWorker(worker: ActorRef): Unit = workerPool += worker

  def isReadyToStart: Boolean = {
    unconnectedSlaves <= 0 && unconnectedWorkers <= 0
  }

  private def unconnectedWorkers: Int = expectedWorkers - connectedWorkers

  private def unconnectedSlaves: Int = expectedSlaves - connectedSlaves

  def removeWorker(worker: ActorRef): Unit = workerPool.remove(worker)

  def slaveConnected(workerCount: Int): Unit = {
    connectedSlaves += 1
    expectedWorkers += workerCount
  }

  def numberOfIdleWorkers: Int = {
    workerPool.size
  }

  // enables iterating over workerPoolInstances
  def apply: Iterator[ActorRef] = workerPool.toIterator

}

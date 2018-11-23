package skynet.cluster.util

import java.io.PrintStream

import akka.actor.ActorRef

import scala.collection.mutable

class WorkerPool(private var slaveCount: Int, localWorkers: Int) {

  val workerPool: mutable.Set[ActorRef] = mutable.Set[ActorRef]()

  val idle: mutable.Set[ActorRef] = mutable.LinkedHashSet[ActorRef]()


  private val expectedSlaves = slaveCount
  private var connectedSlaves = 0
  private var expectedWorkers = localWorkers
  private var connectedWorkers = 0

  def workerConnected(worker: ActorRef): Unit = {
    connectedWorkers += 1
    addWorker(worker)
    println(s"Registered new worker $worker")
    checkPrintStatus()
  }

  def checkPrintStatus(out: PrintStream = System.out): Unit = {
    if (!isReadyToStart) println("Not everybody connected:")
    printStatus()
  }

  def printStatus(out: PrintStream = System.out): Unit = {
    out.println(s"Slaves: $connectedSlaves/$expectedSlaves\nWorkers: $connectedWorkers/$expectedWorkers")
  }

  def addWorker(worker: ActorRef): Unit = {
    workerPool += worker
    idle += worker
  }

  def isReadyToStart: Boolean = {
    unconnectedSlaves <= 0 && unconnectedWorkers <= 0
  }

  private def unconnectedWorkers: Int = expectedWorkers - connectedWorkers

  private def unconnectedSlaves: Int = expectedSlaves - connectedSlaves

  def removeWorker(worker: ActorRef): Unit = {
    workerPool.remove(worker)
    idle.remove(worker)
  }

  def workerAvailable: Boolean = idle.nonEmpty

  def claimWorker(): Option[ActorRef] = {
    val firstIdle = idle.headOption
    firstIdle.foreach(worker => idle.remove(worker))
    firstIdle
  }

  // be careful: using this iterator will remove workers from the idle pool
  val idleWorkerClaimer: Iterator[ActorRef] = new Iterator[ActorRef] {
    override def hasNext: Boolean = workerAvailable

    override def next(): ActorRef = claimWorker().get
  }


  def freeWorker(worker: ActorRef): Boolean = {
    idle.add(worker)
  }

  def slaveConnected(workerCount: Int): Unit = {
    connectedSlaves += 1
    expectedWorkers += workerCount
    println(s"Registered new slave system with $workerCount workers")
    checkPrintStatus()
  }

  def numberOfIdleWorkers: Int = {
    workerPool.size
  }

  // enables iterating over workerPoolInstances
  def apply: Iterator[ActorRef] = workerPool.toIterator

}

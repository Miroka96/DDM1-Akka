package skynet.cluster.actors

import akka.event.LoggingAdapter

// a task message triggers the distribution of work through work messages,
// whose results come back as result messages

@SerialVersionUID(1L)
abstract class TaskMessage {
  def toProcessingState: TaskState
}

@SerialVersionUID(1L)
abstract class TaskState(val message: TaskMessage) {

  var startTime: Long = _
  var endTime: Long = _

  def startProcessing(): Unit = {
    startTime = System.currentTimeMillis()
  }

  def endProcessing(): Unit = {
    endTime = System.currentTimeMillis()
  }
}

@SerialVersionUID(1L)
abstract class WorkMessage {
  def runOn(worker: Worker): ResultMessage

  protected def logSuccess(log: LoggingAdapter): Unit = log.info("Done: {}", this)

  protected def logFailure(log: LoggingAdapter): Unit = log.warning("Failed: {}", this)
}

@SerialVersionUID(1L)
abstract class ResultMessage {

}

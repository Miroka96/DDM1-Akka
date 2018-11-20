package skynet.cluster.actors

import akka.event.LoggingAdapter
import skynet.cluster.actors.ExerciseTask.CSVPerson


// a task message triggers the distribution of work through work messages, whose results come back as result messages

@SerialVersionUID(1L)
abstract class TaskMessage {
  def toProcessingState: TaskState
}

@SerialVersionUID(1L)
abstract class TaskState {
  val message: TaskMessage
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

case class ExerciseTask(persons: Array[CSVPerson]) extends TaskMessage {
  override def toProcessingState: ExerciseTaskState = ExerciseTaskState(this)
}

case class ExerciseTaskState(message: ExerciseTask) extends TaskState

object ExerciseTask {
  case class CSVPerson(
                        id: String,
                        name: String,
                        password: String,
                        gene: String
                      )
}

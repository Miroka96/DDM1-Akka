package skynet.cluster.actors

import akka.event.LoggingAdapter
import skynet.cluster.actors.ExerciseTask.CSVPerson


// a task message triggers the distribution of work through work messages, whose results come back as result messages

@SerialVersionUID(1L)
abstract class TaskMessage {

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

case class ExerciseTask(persons: Array[CSVPerson]) extends TaskMessage

object ExerciseTask {
  case class CSVPerson(
                        id: String,
                        name: String,
                        password: String,
                        gene: String
                      )
}

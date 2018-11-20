package skynet.cluster.actors

import akka.event.LoggingAdapter


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

case class ExerciseTask() extends TaskMessage {

  case class CSVPerson(
                        id: String,
                        name: String,
                        password: String,
                        gene: String
                      )

}

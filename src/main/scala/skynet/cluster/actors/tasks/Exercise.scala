package skynet.cluster.actors.tasks

import skynet.cluster.actors.tasks.ExerciseTask.CSVPerson
import skynet.cluster.actors.tasks.ExerciseTaskState.CrackedPerson
import skynet.cluster.actors.{TaskMessage, TaskState}

case class ExerciseTask(persons: Array[CSVPerson]) extends TaskMessage {
  val maxPassword = 1000000
  val prefixLength = 5

  override def toProcessingState: ExerciseTaskState = ExerciseTaskState(this)
}

case class ExerciseTaskState(override val message: ExerciseTask)
  extends TaskState(message) {

  val persons: Array[CrackedPerson] =
    message.persons.map(csvPerson => CrackedPerson(csvPerson))

}

object ExerciseTaskState {

  case class CrackedPerson(person: CSVPerson) {
    var cleartextPassword: Int = _
    var prefix: Int = _
    var partnerId: Int = _
    var hash: String = _
    var nonce: String = _
  }

}

object ExerciseTask {

  case class CSVPerson(
                        id: Int,
                        name: String,
                        password: String,
                        gene: String
                      )

}
package skynet.cluster.actors.tasks

import skynet.cluster.actors.{ResultMessage, WorkMessage, Worker}


trait PasswordCracking {

  def crack(): ResultMessage = {

    PasswordCrackingResult()
  }
}

case class PasswordCrackingWork() extends WorkMessage {
  override def runOn(worker: Worker): ResultMessage = {
    worker.crack()
  }
}

case class PasswordCrackingResult() extends ResultMessage



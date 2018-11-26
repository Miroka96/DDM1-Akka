package skynet.cluster.actors.jobs

import skynet.cluster.actors.Messages.{LinearCombinationMessage, PasswordCrackingMessage, SubSequenceMessage}

import scala.collection.mutable.ArrayBuffer


object PasswordJob {
  def splitIntoNMessages(workerCount: Int): Seq[PasswordCrackingMessage] = {
    var highestPassword = 1000000
    val stepSize = (highestPassword / workerCount).ceil.toInt
    // -> stepSize * nrOFWorkers >= highestPassword
    val messages = new ArrayBuffer[PasswordCrackingMessage](workerCount)
    while (highestPassword > stepSize) {
      messages.append(PasswordCrackingMessage(highestPassword - stepSize, highestPassword))
      highestPassword -= stepSize
    }
    messages.append(PasswordCrackingMessage(0, highestPassword))
    messages
  }
}

object LinearCombinationJob {
  def splitIntoNMessages(nrOfWorkers: Int, idToPassword: Map[Int, Int]): Seq[LinearCombinationMessage] = {
    Seq(LinearCombinationMessage(idToPassword))
  }
}


object SubSequenceJob {
  def splitIntoNMessages(_nrOfWorkers: Int, numberOfPersons: Int): Seq[SubSequenceMessage] = {
    // TODO might be further splitted up into single pars for working packages
    println(s"number of persons $numberOfPersons")
    (1 to numberOfPersons).map(SubSequenceMessage)
  }
}

object HashMiningJob {
  def splitIntoNMessages(nrOfPersons: Int): Unit = {

  }
}

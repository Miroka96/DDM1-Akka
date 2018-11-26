package skynet.cluster.actors.jobs

import skynet.cluster.actors.Messages.{HashMiningMessage, LinearCombinationMessage, PasswordCrackingMessage, SubSequenceMessage}
import skynet.cluster.actors.WorkManager.{CSVPerson, CrackedPerson}

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
    (1 to numberOfPersons).map(SubSequenceMessage)
  }
}

object HashMiningJob {
  val stepSize = 1000000
  var currentLower = 0

  def splitIntoNMessages(maxPartnerNr:Int,nrOfWorkers:Int) : Seq[HashMiningMessage] = {
    currentLower = stepSize*nrOfWorkers
    (0 until nrOfWorkers).map(i=>HashMiningMessage(maxPartnerNr,i*stepSize,(i+1)*stepSize))
  }

  def giveNextMessage(oldMessage: HashMiningMessage) : HashMiningMessage = {
    val message = oldMessage.copy(from = currentLower + stepSize, to= currentLower+stepSize)
    currentLower += stepSize
    message
  }
}

package skynet.cluster.actors.jobs

import skynet.cluster.actors.Messages.{JobMessage, LinearCombinationMessage, PasswordCrackingMessage}

import scala.collection.mutable.ArrayBuffer

abstract class Job {
  def splitIntoNMessages(nrOfWorkers: Int): Seq[JobMessage]
}

object PasswordJob extends Job {
  override def splitIntoNMessages(workerCount: Int): Seq[PasswordCrackingMessage] = {
    var highestPassword = 1000000
    val stepSize = (highestPassword / workerCount).ceil.toInt
    // -> stepSize * nrOFWorkers >= highestPassword
    val messages = new ArrayBuffer[PasswordCrackingMessage](workerCount)
    while(highestPassword > stepSize){
      messages.append(PasswordCrackingMessage(highestPassword - stepSize,highestPassword))
      highestPassword -= stepSize
    }
    messages.append(PasswordCrackingMessage(0, highestPassword))
    messages
  }
}

object LinearCombinationJob{
  def splitIntoNMessages(nrOfWorkers: Int, idToPassword: Map[Int,Int]): Seq[LinearCombinationMessage] = {
    val highest = Math.pow(2,idToPassword.keys.size).toLong
    var current = 0L
    val stepSize = 10000000
    val messages = new ArrayBuffer[LinearCombinationMessage](nrOfWorkers)
    while(current < highest - stepSize){
      messages.append(LinearCombinationMessage(idToPassword, current, current + stepSize))
      current += stepSize
    }
    messages.append(LinearCombinationMessage(idToPassword, current, highest))
    messages

  }
}
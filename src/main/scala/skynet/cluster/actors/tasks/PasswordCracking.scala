package skynet.cluster.actors.tasks

import java.io.UnsupportedEncodingException
import java.security.{MessageDigest, NoSuchAlgorithmException}

import skynet.cluster.actors.{ResultMessage, WorkMessage, Worker}

trait PasswordCracking {

  def crack(work: PasswordCrackingWork): PasswordCrackingResult = {
    val foundPairs = (work.testpasswordStart to work.testpasswordEnd)
      .flatMap(password => {
        val hash = hashPassword(password)
        work.passwordhashes
          .get(hash)
          .map(userId => (userId, password))
          .toList
      })
      .toMap

    PasswordCrackingResult(foundPairs)
  }

  private def hashPassword(password: Int): String = try {
    val digest = MessageDigest.getInstance("SHA-256")
    val hashedBytes = digest.digest(
      String
        .valueOf(password)
        .getBytes("UTF-8"))

    val stringBuffer: StringBuilder = new StringBuilder(hashedBytes.length)

    for (i <- 0 until hashedBytes.length) {
      stringBuffer.append(
        Integer
          .toString((hashedBytes(i) & 0xff) + 0x100, 16)
          .substring(1))
    }

    stringBuffer.toString
  } catch {
    case e@(_: NoSuchAlgorithmException | _: UnsupportedEncodingException) =>
      throw new RuntimeException(e.getMessage)
  }
}

case class PasswordCrackingWork(passwordhashes: Map[String, Int],
                                // first number to test
                                testpasswordStart: Int,
                                // last number to test
                                testpasswordEnd: Int,
                               ) extends WorkMessage {

  override def runOn(worker: Worker): ResultMessage = {
    worker.crack(this)
  }
}

// @parameter passwords: a map with user id -> password
case class PasswordCrackingResult(passwords: Map[Int, Int])
  extends ResultMessage



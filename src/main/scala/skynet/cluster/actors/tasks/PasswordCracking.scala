package skynet.cluster.actors.tasks

import java.io.UnsupportedEncodingException
import java.security.{MessageDigest, NoSuchAlgorithmException}

import skynet.cluster.actors.Logging

trait PasswordCracking extends Logging {
  def crack(hashesAndIds: Map[String, Int], start: Int, end: Int): Map[Int, Int] = {
    log.info("start cracking")

    (start to end)
      .flatMap(password => {
        val hash = hashPassword(password)
        hashesAndIds
          .get(hash)
          .map(userId => {
            log.info(s"found $hash for $password for $userId")
            (userId, password)
          })
      }).toMap
  }

  def hashPassword(password: Int): String = try {
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




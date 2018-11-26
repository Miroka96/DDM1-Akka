package skynet.cluster.actors.tasks

import java.io.UnsupportedEncodingException
import java.security.{MessageDigest, NoSuchAlgorithmException}
import java.util
import java.util.Random

trait HashMining {
  private def encrypt(partners: Array[Int], prefixes: Array[Int], prefixLength: Int) = {
    val hashes = new util.ArrayList[String](partners.length)
    var i = 0
    while (i < partners.length) {
      val partner = partners(i)
      val prefix = if (prefixes(i) > 0) "1" else "0"

      hashes.add(this.findHash(partner, prefix, prefixLength))

      i += 1
    }
    hashes
  }

  private def findHash(content: Int, prefix: String, prefixLength: Int): String = {
    val fullPrefixBuilder = new StringBuilder

    var i = 0
    while (i < prefixLength) {
      fullPrefixBuilder.append(prefix)
      i += 1
    }

    val rand = new Random(13)
    val fullPrefix = fullPrefixBuilder.toString
    var nonce = 0
    while ( {
      true
    }) {
      nonce = rand.nextInt
      val hash = this.hashPassword(content + nonce)
      if (hash.startsWith(fullPrefix)) return hash
    }
    throw Exception
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

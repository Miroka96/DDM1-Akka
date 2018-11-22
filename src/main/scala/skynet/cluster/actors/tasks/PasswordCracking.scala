package skynet.cluster.actors.tasks

import java.io.UnsupportedEncodingException
import java.security.{MessageDigest, NoSuchAlgorithmException}

import scala.collection.mutable

trait PasswordCracking {


  def crack(hashesAndIds: Map[String, Int], start: Int, end: Int): Map[Int, String] = {
    // This should be wrapped in  a future but I am not sure if we need a special dispatcher and how we send back results

    println("start cracking")
    val resultMap = mutable.Map[Int, String]()

    // Todo this could be nicer and directly generate the map
    (start to end).foreach(password => {
      val hash = hashPassword(password)
      println(password, " hash", hash)
      hashesAndIds.get(hash).foreach(id => {
        println("found", hash, password, id)
        resultMap += ((id, hash))
      })
    })

    resultMap.toMap
  }

  def test(): Unit ={
    println("hola")
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




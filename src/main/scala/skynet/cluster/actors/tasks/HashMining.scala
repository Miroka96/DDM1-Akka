package skynet.cluster.actors.tasks

import java.io.UnsupportedEncodingException
import java.security.{MessageDigest, NoSuchAlgorithmException}
import java.util
import java.util.Random

trait HashMining {

  /**
    * Why this is correct: For all numbers i smaller than the larges partner number lp, the nonce would simply be
    * i_nonce = lp - i +lp_nonce
    * Thus all the hashes are correct and there are only two necessary hashes, one for 00000 and one for 11111
    * as all nonces can be computed from the one master nonce lp_nonce
    * @param maxPartnerId
    * @param start
    * @param end
    * @return
    */
  def mine(maxPartnerId: Int, start: Int, end: Int): String = {
    var current = start
    while(current <= end){
      val hash = this.hashPartnerNr(maxPartnerId + current)
      if (hash.startsWith("00000")){
        println("universal nonce for 0 ", current)
        return hash
      }
      if (hash.startsWith("11111")) {
        println("universal nonce for 1 ", current)
        return hash
      }
      current+=1
    }
    return ""
  }

  private def hashPartnerNr(partnerNr: Int): String = try {
    val digest = MessageDigest.getInstance("SHA-256")
    val hashedBytes = digest.digest(
      String
        .valueOf(partnerNr)
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

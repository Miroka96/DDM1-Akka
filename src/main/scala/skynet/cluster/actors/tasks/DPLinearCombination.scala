package skynet.cluster.actors.tasks

import scala.collection.mutable.ArrayBuffer

object DPLinearCombination {


  def reconstructSubset(dp: Array[Array[Boolean]], passwords: Array[Int], i: Int, sum: Int, subset: ArrayBuffer[Int]): ArrayBuffer[Int] = {
    //last element does the job
    if(i==0 && sum != 0 && dp(0)(sum)){
      subset.append(passwords(i))
      subset.foreach(println(_))
    }
    // done
    else if(i == 0 && sum == 0){
      subset.foreach(println(_))
    }

    //element not needed for sum
    else if (dp(i - 1)(sum)) {
      reconstructSubset(dp, passwords, i - 1, sum, subset)
    }
    //element is needed if (sum >= passwords(i) && dp(i - 1)(sum - passwords(i)))
    else {
      subset.append(passwords(i))
      reconstructSubset(dp, passwords, i - 1, sum - passwords(i), subset)

    }
    subset
  }

  def getSubset(passwords: Array[Int]): Unit = {
    val sum = passwords.sum / 2
    var dp = Array.fill(passwords.length) {
      Array.ofDim[Boolean](sum + 1)
    }
    for (i <- dp.indices) {
      dp(i)(0) = true
    }
    if (passwords(0) <= sum)
      dp(0)(passwords(0)) = true

    for (j <- 1 until passwords.length) {
      for (k <- 0 to sum) {
        dp(j)(k) = if (passwords(j) <= k) dp(j - 1)(k) || dp(j - 1)(k - passwords(j)) else dp(j - 1)(k)
      }
    }
    if (!dp(passwords.length - 1)(sum)) {
      println("no solution")
      return null
    }
    var subset = new ArrayBuffer[Int]()
    reconstructSubset(dp, passwords, passwords.length - 1, sum, subset)
  }

  def main(args: Array[String]): Unit = {
    getSubset(Array(240492,
      221100,
      800375,
      183998,
      131363,
      355710,
      354693,
      163213,
      412114,
      737060,
      955316,
      388905,
      319162,
      548154,
      268044,
      131769,
      984860,
      336024,
      196478,
      139452,
      115869,
      411640,
      269937,
      904944,
      407282,
      602011,
      554168,
      324423,
      557375,
      950394,
      279181,
      721108,
      567287,
      248909,
      808878,
      781820,
      659033,
      945951,
      880950,
      147667,
      327056,
      536785))
  }
}

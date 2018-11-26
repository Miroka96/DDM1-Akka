package skynet.cluster.actors.tasks

import scala.annotation.tailrec
import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer

trait DPLinearCombination {

  private def reconstructSubset(dp: Array[Array[Boolean]], passwords: Array[Int], i: Int, sum: Int, subset: ArrayBuffer[Int]): ArrayBuffer[Int] = {
    //last element does the job
    if (i == 0 && sum != 0 && dp(0)(sum)) {
      subset.append(passwords(i))
      subset.foreach(println(_))
    }
    // done
    else if (i == 0 && sum == 0) {
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

  def getSubset(passwords: Array[Int]): ArrayBuffer[Int] = {
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

  def solveLinearCombination(idToPwd: Map[Int, Int]): Map[Int, Int] = {
    println("Solving linear combination")
    val passwords = Array.ofDim[Int](idToPwd.keys.size)
    for (id <- idToPwd.keys) {
      passwords(id - 1) = idToPwd(id)
    }
    val idToIndex = mutable.Map() ++ idToPwd.mapValues(_ => 1)
    val subset = getSubset(passwords)
    for(password <- subset){
      var id = idToPwd.find(_._2==password).get._1
      idToIndex(id) = -1
    }
    println(idToPwd)
    println(idToIndex)
    idToIndex.toMap
  }

}

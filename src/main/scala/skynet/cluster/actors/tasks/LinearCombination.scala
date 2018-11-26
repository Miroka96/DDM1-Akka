package skynet.cluster.actors.tasks

import scala.collection.mutable

trait LinearCombination {
  def solveLinearCombination(idToPwd: Map[Int,Int], start: Long, stop: Long) : Map[Int,Int] = {
    println(s"start solving linear from $start to $stop")
    val passwords = Array.ofDim[Int](idToPwd.keys.size)
    for(id <- idToPwd.keys){
      passwords(id-1)=idToPwd(id)
    }
    var x: Long = start
    while(x <= stop){
      var binary: String = x.toBinaryString
      val prefixes =  Array.fill(passwords.length)(1)
      for(j <- binary.length() - 1 to 0){
        if (binary.charAt(j) == '1'){
          prefixes(j) = -1
        }
      }
      if (this.sum(passwords, prefixes) == 0) {
        println(x)
        val result: mutable.Map[Int,Int] = mutable.Map.empty
        for(i <- prefixes.indices){
          result.+=((i+1, prefixes(i)))
        }
        return result.toMap
      }
      x+=1
    }
    null
  }

  def sum(passwords: Array[Int], prefixes: Array[Int]): Int = {
    var sum = 0
    for (i <- passwords.indices) {
      sum += passwords(i) * prefixes(i)
    }
    sum
  }
}




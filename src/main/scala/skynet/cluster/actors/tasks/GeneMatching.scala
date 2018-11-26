package skynet.cluster.actors.tasks

import skynet.cluster.actors.WorkManager.CSVPerson

trait GeneMatching {
  def subsequenceLengths(pairs: Seq[(String, String)], alg: Int): Array[Int] = {
    pairs.map { case (s1, s2) =>
      alg match {
        case 0 => longestCommonSubstring(s1, s2).length

        case 1 => longestCommonSubsequenceDP(s1, s2).length

        case 2 => longestOverlap(s1, s2).length

        case 3 => longestOverlapOriginal(s1, s2).length

      }

    }.toArray


  }

  // 8000 ms
  private def longestCommonSubstring(a: String, b: String): String = {
    def loop(bestLengths: Map[(Int, Int), Int], bestIndices: (Int, Int), i: Int, j: Int): String = {
      if (i > a.length) {
        val bestJ = bestIndices._2
        b.substring(bestJ - bestLengths(bestIndices), bestJ)
      } else {
        val currentLength = if (a(i - 1) == b(j - 1)) bestLengths(i - 1, j - 1) + 1 else 0
        loop(
          bestLengths + ((i, j) -> currentLength),
          if (currentLength > bestLengths(bestIndices)) (i, j) else bestIndices,
          if (j == b.length) i + 1 else i,
          if (j == b.length) 1 else j + 1)
      }
    }

    loop(Map.empty[(Int, Int), Int].withDefaultValue(0), (0, 0), 1, 1)
  }


  /**
    * Find longest common subsequence using Dynamic Programming
    */
  // 150 ms
  def longestCommonSubsequenceDP(s1: String, s2: String): String = {
    if (s1 == null || s1.length() == 0 || s2 == null || s2.length() == 0) ""
    else if (s1 == s2) s1
    else {
      val up = 1
      val left = 2
      val charMatched = 3

      val s1Length = s1.length()
      val s2Length = s2.length()

      val lcsLengths = Array.fill[Int](s1Length + 1, s2Length + 1)(0)

      for (i <- 0 until s1Length) {
        for (j <- 0 until s2Length) {
          if (s1.charAt(i) == s2.charAt(j)) {
            lcsLengths(i + 1)(j + 1) = lcsLengths(i)(j) + 1
          } else {
            if (lcsLengths(i)(j + 1) >= lcsLengths(i + 1)(j)) {
              lcsLengths(i + 1)(j + 1) = lcsLengths(i)(j + 1)
            } else {
              lcsLengths(i + 1)(j + 1) = lcsLengths(i + 1)(j)
            }
          }
        }
      }

      val subSeq = new StringBuilder()
      var s1Pos = s1Length
      var s2Pos = s2Length

      // build longest subsequence by backtracking
      do {
        if (lcsLengths(s1Pos)(s2Pos) == lcsLengths(s1Pos - 1)(s2Pos)) {
          s1Pos -= 1
        } else if (lcsLengths(s1Pos)(s2Pos) == lcsLengths(s1Pos)(s2Pos - 1)) {
          s2Pos -= 1
        } else {
          assert(s1.charAt(s1Pos - 1) == s2.charAt(s2Pos - 1))
          subSeq += s1.charAt(s1Pos - 1)
          s1Pos -= 1
          s2Pos -= 1
        }

      } while (s1Pos > 0 && s2Pos > 0)

      subSeq.toString.reverse
    }
  }

  // 110 ms
  private def longestOverlap(s1: String, s2: String): String = {
    var str1 = s1
    var str2 = s2
    if (str1.isEmpty || str2.isEmpty) return ""
    if (str1.length > str2.length) {
      val temp = str1
      str1 = str2
      str2 = temp
    }

    var currentRow = new Array[Int](str1.length)
    var lastRow = if (str2.length > 1) {
      new Array[Int](str1.length)
    } else null

    var longestSubstringLength = 0
    var longestSubstringStart = 0

    for (str2Index <- 0 until str2.length) {
      val str2Char = str2.charAt(str2Index)

      for (str1Index <- 0 until str1.length) {
        var newLength = 0
        if (str1.charAt(str1Index) == str2Char) {
          newLength = if (str1Index == 0 || str2Index == 0) 1
          else lastRow(str1Index - 1) + 1
          if (newLength > longestSubstringLength) {
            longestSubstringLength = newLength
            longestSubstringStart = str1Index - (newLength - 1)
          }
        }
        else newLength = 0
        currentRow(str1Index) = newLength
      }

      val temp = currentRow
      currentRow = lastRow
      lastRow = temp

    }
    str1.substring(longestSubstringStart, longestSubstringStart + longestSubstringLength)

  }

  // 30 ms
   def longestOverlapOriginal(s1: String, s2: String): String = {
    var str1 = s1
    var str2 = s2
    if (str1.isEmpty || str2.isEmpty) return ""
    if (str1.length > str2.length) {
      val temp = str1
      str1 = str2
      str2 = temp
    }
    var currentRow = new Array[Int](str1.length)
    var lastRow = if (str2.length > 1) new Array[Int](str1.length)
    else null
    var longestSubstringLength = 0
    var longestSubstringStart = 0
    var str2Index = 0
    while ( {
      str2Index < str2.length
    }) {
      val str2Char = str2.charAt(str2Index)
      var str1Index = 0
      while ( {
        str1Index < str1.length
      }) {
        var newLength = 0
        if (str1.charAt(str1Index) == str2Char) {
          newLength = if (str1Index == 0 || str2Index == 0) 1
          else lastRow(str1Index - 1) + 1
          if (newLength > longestSubstringLength) {
            longestSubstringLength = newLength
            longestSubstringStart = str1Index - (newLength - 1)
          }
        }
        else {newLength = 0}
        currentRow(str1Index) = newLength

        str1Index += 1

      }
      val temp = currentRow
      currentRow = lastRow
      lastRow = temp


      str2Index += 1



    }
    str1.substring(longestSubstringStart, longestSubstringStart + longestSubstringLength)
  }

  def findPartner(dataSet: Array[CSVPerson], id: Int, gene: String): Int = {
    var longest = 0
    var longestLength = 0
    for (datum <- dataSet) {
      if (datum.id != id) {
        val length = this.longestOverlapOriginal(gene, datum.gene).length
        val did = datum.id
        //println(s"lenght was $length with my $id $did")
        if (length > longestLength) {
          longestLength = length
          longest = datum.id
        }
      }
    }
    longest
  }

}

object Test extends GeneMatching {
  def main(args: Array[String]): Unit = {
    val data = List(("GGUCUCGAAGGGUGAACAAGCGACCUCAGAUCGUUGGCCUUCACCCGCACAGCGGUUGCCGCGUAUAAGGGGUAGGGAUACAUUUACCUGCAACUGACCCUGUCACAUCUAGUCCCUUGUUCUGCUGCCGCGCAAUUGUUCAUCGUUACGAUUAGUGGAGGACCUAACCUCAGCUGUCUGUUGGGUGAUAACUGUUUGGAGUCUUCUGCGACCCCCGGAACCGUGUCUCUCCGUCAGCGCUUCCCCAAUGUCCGGCAACCGUAGCUCGUCCCAAGUCUUUUGAGACAUAUAUCGAUGGCCAGCACCCGAAUCCUGUCCAGGGGGAUAGCAAGUUCUUCUUGGCAACAGAGCAAGAAUUGGACAUAUUGCCAUAAACAUUACCCGACUAACCCAGCAGCGAGUCAAAAGAGCGUAGUCCCUAUACGCCCCCGGCUGCAUUUAGUCGGAGAACAUCAGAUAAAAACGCCGUUUGACACGUAUAAGUCCCGCUCACUAUAUAUAGUCGGCAUAGAGUAUCGGUAUCUGCGGUAUGAGGUGAAACCUUCUUAGAGACAGCGACGUUUUGAUUAUGGGAUGCUGAUCACGAGAGUGUCACGGAAUGUACUCCUCCAAGGCCGCGUUAGAGUGCGGACGUAUGUACUAUGAUCAAUUAGGGCCAUGCAUUCACAAUUCUAACUCGGCGUGGGGCGGUAACUCAAGCCUGCCUAGCUCUACGUUACGUAGUCGUCGCUUGAUGGGUGACCUAAGACAGGGUGGAGAGAAAAUCGAUGCUACUAAGUACGAAUAGAAGGGAGGUGUCGGUAGGUACGUUGAACGGUACCGCUUUGAUUUUUUAACUUUAUCGUAUCUCCUGCUUGUUUAAAGCGCAAGUGGAGGUUUUGGGAUGAUUAGAAAGUAGUUCAUAUGUCUUAAAGUCGAGCGUCCCCAGGCGUAUGCCUAUAACCGCAGCUAAUCUGCAUUCACUGGCGGUACCCUUUCGGGUUCGUUUUAGGUCGAUCCAAGUACUAAACGAGUUGCGAUAUGAGUUGUUUAGAGAAAAGUGGUCGUACAAUACAAAUUAUCCUUUCCCUCACAAUGUCUAGGUUUAUCCACGCCUAACUCUAGUCCCAGCUUGUUCGUCUUGUAAUGAAGCUGACAGCGAACUCUCCAGCAAUUUUUUCACCCAAUCCUGAAUUAGAGACCGACCAGGCGGGAGGCUACAUCUGGAUGUCUAAGGGCCUGAGGACGCCUAUUCGCUUAUGUCUCUUACUCCGUGUUCGUUGCCCAUUAUGAUCUUGCCAUAUUCAUGCUCACCGCAUUGCCAACGGCCGGUUAACCGCCGAAGGUUGCGGGGCCAACCGCGCUGAUGAGUUUAUUAUACGCUUCCCACCCGCCCUCCGGCAGUCGACUCCUGACCUUCUGUCUAUCGCUGCAGCACGACGCUGCAUAUUGGAACACUCGCCCGCAGAGGAGAGUUGCUUGCCUCUUGCAAAAUGAAAGUGGCAGAGGCGCGUUGAUUAAACGCAGCUAUUGGAUGAAAACGUCUACUUAUCGUCCUUGAUCGGGAUGAGGCUCACGACAACUGACCCCGUGUGAAGCUAAUAUCCGUGGAGGUUCAGACCGGCUGCUCCAAAUGAACUCCUUACGAGCUGGGGUUAUCAUAAUUACGCACUUGACCGAUAGACUCUGCGGAAUGCACGAUUAUCUGCGAGCCGAGUGCCCUUUUUCUGGGUAGCGAUCACUCGCACACGUCCACCCUAAAAGCUCAGUCGACUUGGCACUAGCCUUGGCAACAGAGUCAUAGGUAUAUAUGUACUUUUAUCUCUCAGCCUGAAAAAACCAGGAGACGUGUAAAUAGUGUAGUUCUACUCUGGACGGAACCUUUUUCGACAGUGCCUCAAAACCUAUUGACAAUUUGAUCCAUUGGACGGACCGGGUGCCGGCUGCGCCGCGCUGAUGUUAAUGCCCGAACACACGCACAAUACACGCCUGCUAGGGCACCCUAAAGAACAUCAGCAGAACGAUCGUGGGUUGCCCAUACGGACGACCCCGCACGUAACAAAGACACUAUCUACGUCCGAAGAGGAGUGAUAAUUUGCAUUACCUAAAGCCCGUGAAGCUUUAUAGAAUCCUUGUUUGGCCGUACCCCCGAGCUCUACGCCCCAAUAAUACAAUGUGGUACUAUAUUAGUGUUUCGCUGGAGUGAGCCUAGCCCAAACCCGUUUAGGAGCACUUAUUGGGCCUUAGUACGCUACUAGUAAUGAUAGCUAUGGCAGGAAAGGUACGACAAAAUGUACAGUCAUUAGAGCCGAGCUUCCCGUGGGCUGACUGGCGCCCUAGUUGAUCACAGUAGCGCUUGCCACCCUAGCAAGUCAAUGCUGGUCAAGGAGAGUCUUCGGGCCCGAUCAACCGGUUGAGAUCCACAGACAUCUAAACAUGGUUUGUGUACUGGCCUACUCGGGUUACGAAGUCACGGGCAUUACAUAAUUGUAGAGGGCUUGACUAGGCCAAUGAUAUCGAAUUUUUAGAUGUCCUGAGCCCCCGAGAGACAACACUCCUAGCGAUUGACGCUCUUCAGUCGCAAUUGAGACCACCCUGACAAAACCUCGCCCCGGCCCCGGAGUUCAUGACGCUCACAGUUGUCGACAGCUCUUACGAAACACCGAUCCGUUGAGCUACUCAGAGGCCACGGCGACGCACCGAGACAGCCAGACUUGAAUACACAGUCGAAGGAUAUUUCCAGUGUAUUAGGAUUGCUACGCUUCGUUCUAAAGCAUUGUUCUAGUUCUCCAAGGGAACGAGGGCAAAAAUACGGGGGGAAUUGACCUUCGGGCAGAUGAUGCGCCUCCUGGACGGUAAUGUUGAUGGAGCCAAUUAUCACUGAGCAAGCACACACUUUUUCUACUUUGUUUGAAAUCAGCUAGGAGACAUCCUAACCUGACUGCCAGAGACUCAGAGUCUGUUUGAGCAAGCCACGAGGUUGUGCACAAGUCAAGCUUACGCAAUUCCUGUGUCGCCGCA", "CGGGGGGAGAGAAAACUUUCAACUCCUAUACACCUAUGCUUCCUUAUCGUAACUGAUGCUCGCAGGUGUUUGUGAUCCGAACGAUAGACCUGGAUUUUGCCGGUCAUGACAAUCCCCAUACUGAACAGUUUCCACAAGUUACAGCAUGUCCCGUUUUGUCUAUGCGAGGGGGUUGUGAAUCAAUGUUAGUAGGAGUACGGAGACAUACUCAUUUCAACCAUUUGCGGUGCUUCAUCGAAUAGAUACAUGUCACCAAACUAUGUUCCGAGGGCGUCAGACACAGCCUGCUCGUUCGCGUAAAAGGGCGACCGGGAUCUUAACAUACGUUUACUGACAAAGCGAACCGCUACGUGGCAACUCUUAGAGUUUAGCAUUGACCAACGCAGUCAUGUUAGCGUAAAAACCUAGCUAACUGCAGAGUCGCAGUAGUACCAAAGCGCUCCAAUUCGCAAAUCUACUACGGGCGUCUUGGACGUCAUUGGAUCUGGCCCUAAGUAUCCGCAAGUUAAACAACAAAAUGCAAGAAGCUUACUACCGUCGACGGUUCAACUUUGUUCACCAAGCUAGUUGAACUGAUGCUCACAAUCGCAUCAGCAGGUGACAAGAUAAAUUCGAGGUUCCUUGUAUGACUGCGAAAGUCGUCAUAAUCACGAUAUAUUCUGACUAUCGACUGUCUGCGAACCCUAGAUACCUGAACCGGAAUGAUGCGUUGUAGAAUCUUCGUUGGUAACGGAAUGGCUUGGGAUCCAUUAUAAGAGCUAAUCGUGUACGAUUUCACAGCUCCAUCCCGCCGGUCCACUGACUAUCCCCACAAUAUAAUGCCAUACAAGGACGCCGGAGCUAGCCAUGAGGCCAGUGUGUGAUCGGGAGCGCGAGAAGACGCAUCUCUGCCAUUUGUUUUCUUCGAAUGAUCACGAUAAGUAUGAUUCUCCAACCGAUAAAUAUUACGCAAUACACAUGCGCCUAUUGUGUCACCGUAGCCUAACUUCCUCACUGCCACGUUAUAGAGUUCUAAAGGAGUUUCUUCGGUCUGGAUUGGAACUUCACCAGUUUCCUGACGCGCCAGGAUCUUUACUCACCUGCCACUGCUUCUGGGUUCUACACGCGCCAGUCGCCAGAGCAACACUUGGGUUCAGAAGGAUGCAUUUAGGUUAGCCCGCAGGACCGCCCCUUCCAAGCUUCCGAACCGCUUCACGAACUGUUGGGUUACCUAACAUGAUUACCAAUGCUUUAGGUUGCCGCCAUUUCCAACUUCUUAGCAUCUAAUCGAGCCGUUAUGGACAGUUCAAUUGACGUGUACCUGCUCAGUGCCUCCAUGCUUCACACGUCCUGGUACCCCUGUCCACUAUAGGGGUGUGGAUCAUGAAGGUCGUGGAGCACCCCUCUUUCCUCUGACCUUAAUACAAAGCAAUACCAAUAAGGGCUCCCCGUCAAUAUACCCGUUCGACACGAUGGUGUAAGACAGCGGAGGUUUGGUACAGCUAAAUGUUGAGCAUAACUCCAACGGUAGUUACAGUAUGAGCUAGCAACAGUCACGAGUAUCUACACUGCUUGGCUCUAGACCCACUUAAACGCACGGGACAUGCGUCUUGACUAUUUCCGUGUAUCUCUGGACAAUAUUCUCCGACAUACUGUCGUUCGAAAGUGGCGAGGCAAAGGAAUCUUUUCCUUCACCGCAAGUGGGUACGGUCAACAGGCUACGCGCAUUGAUUCAAACCAACAACAUUUCCAGCACUGAUACCACUCCACGGGAGCCCGUUGCCGCCUGAGGAAUGUAUGUAUGCCAUCGAGCAGCGUCAGCACUAUUCCGAAGUCGCUCGUGGAAUAUCACCCCGGCUGAGAUAAAGCCUUCGGGCCGCGGGAUCUGGCCAGAGAUCCCUUGACUGUCCUAUGAAUUGACCUUGAAGGCGAUUAACAUCUGCGCAAGGCAUAGCGCGUAUGACGAACCUACGGACCAGGCACACAGCGAUCUAUUGAAUCUCGGCAUAGGUGGGGGUUGUCUACGCCUAGUUUUAAGGCGGCUUCUGGCUCACUGAUCCUGCGUCUCCCCAUACGUUAGAUCUCAACUCGGAGUAAUAUGAAAUCGGCAUUGCGGCACACAGCGAGAAUAGACGGUUGGUGUUAAGAAAGUUCAAUUGUGAGCACGUUUCACAUUCCUACAUUUCCAGAUAUACCAUACUCCUCUUUCUGUCCCGAUCCCUGAACCUAAAACCUAUCGUGACCGGGCCAGAGGUAUAUAAGCGCGAAUGUUCAUAAUUGUCAGCAUCGUAUUCCCUACCAACAUAAGCGUGGAUGUAAAAAGUAUUCAGAACACAGGUUUGGUAACGGUGCAAAACACCUCGCAGCCCUAGAAUCCCCUCAAAGGCUUCAUCGUGUUAAUUCUCGUCAACCUGGAGGCCCUCAAAAAGGAAAGACCGGAAGACCACCUAAUCGAAUAUUGGACACAUUCUUCGUGGAAUCCGAUGGCGGUCAUCAGUGUUGUUCCUGGCUCGGGUGACCCCUUGGAUCCAGGCCGCUGGAAUGCUAGGCGGGAUAUAUAGUUAAGCCUCCGGCCUUGGAUCAAUCCUAAAUGGGUGUAGCCGUUGUAGUCACGCUGAACAGAUGAUGUACUGACGUUAACCAACUGAUCUAAGCGGGCUGUGGCGACAUAUAAAAGGACUGUGUAGAUCAAAGCUCUGGCCCCGAUAGCAGGUGCGCCGACUAGGCCUUCGGGUGAGACGAGAGGCAGCUUAGUGGCUAGCGUACGGUUUCGAAGUUAGGUUGAUAAUCCGACACCGGACCUCGAACGUCGUAUGAACGAGGAUCAUGUGUUUAUCCGAAUUUUCAAACUUUAUUACCGAGCCAGAGAGCUUUGAUUGUUGUAAUAAGAGGUCUAAUUCACAUGUGAACGGUUGAUCCGAAAAGUGGGCCGUGAGUCCCGCGAAAGGUGAGCGUGUUCGGCUCCAGCGACUCCGAGAUAAAAGUUUACUGUUGUGGGACGUCGAUAACGAUACGCCCUGU"))

    (0 to 32).foreach(alg => {
      println(alg % 3 +1)
      Thread.sleep(1000)
      val start = System.currentTimeMillis()
      val first = this.subsequenceLengths(data, alg % 3 +1)
      val end = System.currentTimeMillis()
      println(end - start)
      println()
    })


  }
}
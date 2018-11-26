package skynet.cluster.actors.tasks

import skynet.cluster.actors.WorkManager.CSVPerson

trait GeneMatching {

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

  // 30 ms
  private def longestOverlapOriginal(s1: String, s2: String): String = {
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
    } else {
      null
    }
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
        else {
          newLength = 0
        }
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


}

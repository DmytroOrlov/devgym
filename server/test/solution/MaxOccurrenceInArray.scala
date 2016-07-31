package solution

import service.reflection.PositiveOutput
import tasktest.{MaxOccurrenceInArraySolution, MaxOccurrenceInArrayTest}

class TTMaxOccurrenceInArray extends PositiveOutput {
  behavior of "ScalaTestRunner for MaxOccurrenceInArray"

  override val suiteInstance = new MaxOccurrenceInArrayTest(new MaxOccurrenceInArray)
}

class MaxOccurrenceInArray extends MaxOccurrenceInArraySolution {
  def maxOccurrence(uniqueNum: Int, a: Array[Int]): Int = {
    val count = new Array[Int](uniqueNum + 1)
    var maxOccurrence = 1
    var index = 0

    for (i <- a.indices) {
      if (count(a(i)) > 0) {
        val tmp = count(a(i)) + 1
        if (tmp > maxOccurrence) {
          maxOccurrence = tmp
          index = i
        }
        count(a(i)) = tmp
      } else {
        count(a(i)) = 1
      }
    }

    a(index)
  }
}
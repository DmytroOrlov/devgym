package solution

import service.PositiveOutput
import tasktest.{MinDifferenceInArraySolution, MinDifferenceInArrayTest}

class TTMinDifferenceInArray extends PositiveOutput {
  behavior of "ScalaTestRunner for MinDifferenceInArray"

  override val suiteInstance = new MinDifferenceInArrayTest(new MinDifferenceInArray)
}

class MinDifferenceInArray extends MinDifferenceInArraySolution {
  def minDifference(a: Array[Int]): Int = {
    val sa = a.sorted
    var diff = Math.abs(sa(0) - sa(1))

    for (i <- sa.init.indices) {
        val temp = Math.abs(sa(i) - sa(i + 1))
        if (temp < diff)
          diff = temp
    }
    diff
  }
}

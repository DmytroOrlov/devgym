package solution

import org.scalatest.{Matchers, FlatSpec}
import service.ScalaTestRunner
import tasktest.{SubArrayWithMaxSumTest, SubArrayWithMaxSumSolution}

class TestForTest extends FlatSpec with Matchers {
  behavior of "ScalaTestRunner for SubArrayWithMaxSum"

  it should "pass all tests for correct solution" in {
    val suiteInstance = new SubArrayWithMaxSumTest(new SubArrayWithMaxSum)
    ScalaTestRunner.execSuite(suiteInstance) shouldNot include regex ScalaTestRunner.failedMarker
  }
}

//solution
class SubArrayWithMaxSum extends SubArrayWithMaxSumSolution {
  def apply(a: Array[Int]): Array[Int] = {
    var currentSum = 0
    var maxSum = 0
    var left, right = 0
    var maxI = 0 //used when all negatives in the array

    for (i <- a.indices) {
      val incSum = currentSum + a(i)

      if (incSum > 0) {
        currentSum = incSum

        if (currentSum > maxSum) {
          maxSum = currentSum
          right = i
        }
      } else {
        left = i + 1
        right = left
        currentSum = 0
        if (a(i) > a(maxI)) maxI = i
      }
    }

    if (left == a.length) a.slice(maxI, maxI + 1)
    else a.slice(left, right + 1)
  }
}




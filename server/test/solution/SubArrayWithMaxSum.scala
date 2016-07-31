package solution

import service.reflection.PositiveOutput
import tasktest.{SubArrayWithMaxSumSolution, SubArrayWithMaxSumTest}

class TTSubArrayWithMaxSum extends PositiveOutput {
  behavior of "ScalaTestRunner for SubArrayWithMaxSum"

  override val suiteInstance = new SubArrayWithMaxSumTest(new SubArrayWithMaxSum)
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

    if (left == a.length) Array(a(maxI))
    else a.slice(left, right + 1)
  }
}

package tasktest

import org.scalatest.{FlatSpec, Matchers}

class SubArrayWithMaxSumTest(solution: SubArrayWithMaxSumSolution) extends FlatSpec with Matchers {
  behavior of "SubArrayWithMaxSum"

  it should "return max sum sub array within given array" in {
    solution.apply(Array(-2, 1, -3, 4, -1, 2, 1, -5, 4)) should be(Array(4, -1, 2, 1))
    solution.apply(Array(-2, 1, -3, 4, -1, 2, 1, 5, 4)) should be(Array(4, -1, 2, 1, 5, 4))
    solution.apply(Array(2, -1, 0, 0, 0, 0, 1)) should be(Array(2))
  }

  it should "return the whole array when given array has only positive numbers" in {
    solution.apply(Array(2, 1, 3, 4, 1, 2, 1, 5, 4)) should be(Array(2, 1, 3, 4, 1, 2, 1, 5, 4))
  }

  it should "return max sum sub array when given array contains only negative numbers" in {
    solution.apply(Array(-2, -1, -3, -4, -1, -2, -1, -5, -4)) should be(Array(-1))
    solution.apply(Array(-2, -3, -3, -4, -6, -2, -6, -5, -1)) should be(Array(-1))
  }
}

trait SubArrayWithMaxSumSolution {
  def apply(a: Array[Int]): Array[Int]
}

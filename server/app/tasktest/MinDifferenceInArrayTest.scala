package tasktest

import org.scalatest.{FlatSpec, Matchers}


class MinDifferenceInArrayTest(solution: MinDifferenceInArraySolution) extends FlatSpec with Matchers {

  it should "return 0 as min difference in array with non-unique numbers" in {
    solution.minDifference(Array(1, 2, 2, 2, 3, 12)) should be(0)
  }

  it should "return non 0 as min difference in array with unique numbers" in {
    solution.minDifference(Array(1, 4, 10, 22, 43, 12)) should be(2)
  }

  it should "return min difference in array with negative numbers" in {
    solution.minDifference(Array(1, 4, 10, 22, 43, -12)) should be(3)
  }
}

trait MinDifferenceInArraySolution {
  def minDifference(a: Array[Int]): Int
}

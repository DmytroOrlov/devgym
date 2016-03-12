package tasktest

import org.scalatest.{FlatSpec, Matchers}

class MaxOccurrenceInArrayTest(solution: MaxOccurrenceInArraySolution) extends FlatSpec with Matchers {
  it should "return max occurrence in array" in {
    solution.maxOccurrence(3, Array[Int](1, 2, 2, 3, 3, 3, 1)) should be(3)
    solution.maxOccurrence(3, Array[Int](1, 1, 1, 2, 2, 3, 3)) should be(1)
  }

  it should "return max occurrence in array of 1 element" in {
    solution.maxOccurrence(1, Array(1)) should be(1)
  }

  it should "return max occurrence in array of equal count of max occurrence" in {
    solution.maxOccurrence(3, Array(1, 2, 2, 3, 3)) ensuring (m => m == 2 || m == 3) //improve
  }
}

trait MaxOccurrenceInArraySolution {
  def maxOccurrence(M: Int, A: Array[Int]): Int
}


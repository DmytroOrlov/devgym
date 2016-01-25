package tasktest

import org.scalatest.{FlatSpec, Matchers}

class UniqueNumbersInArrayTest(solution: UniqueNumbersInArraySolution) extends FlatSpec with Matchers {
  it should "return count of unique numbers in array " in {
    solution.uniqueNumbers(Array(4, 2, 2, 32, 3, 32, 2)) should be(4)
  }

  it should "return count of unique numbers in array of equal numbers " in {
    solution.uniqueNumbers(Array(2, 2, 2, 2, 2)) should be(1)
  }

  it should "return count equals array length when all numbers are unique " in {
    solution.uniqueNumbers(Array(2, 1, 3, 4, -4)) should be(5)
  }
}

trait UniqueNumbersInArraySolution {
  def uniqueNumbers(a: Array[Int]): Int
}

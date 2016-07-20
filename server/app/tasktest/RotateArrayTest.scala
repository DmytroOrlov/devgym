package tasktest

import org.scalatest.{FlatSpec, Matchers}

class RotateArrayTest(solution: RotateArraySolution) extends FlatSpec with Matchers {
  val inA = Array(3, 4, 5, 7, 4, 2, 5, 7, 1, 40)

  it should "rotate array from left to right on 5 elements. Length is 10" in {
    solution.rotate(inA, 5)
    inA should be(Array(2, 5, 7, 1, 40, 3, 4, 5, 7, 4))
  }

  it should "return array as is if steps is 0" in {
    val initialA = inA.clone()
    solution.rotate(inA, 0)
    inA should be(initialA)
  }

  it should "return original array if steps is equal to array length" in {
    val initialA = inA.clone()
    solution.rotate(inA, inA.length)
    inA should be(initialA)
  }

  it should "rotate array even if steps is greater than array length" in {
    val a = Array(1, 2)
    solution.rotate(a, 3)
    a should be(Array(2, 1))
  }
}

trait RotateArraySolution {
  def rotate(a: Array[Int], steps: Int)
}

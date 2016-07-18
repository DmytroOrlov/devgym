package solution

import service.PositiveOutput
import tasktest.{RotateArraySolution, RotateArrayTest}

class TTRotateArray extends PositiveOutput {
  behavior of "ScalaTestRunner for RotateArray"

  override val suiteInstance = new RotateArrayTest(new RotateArray)
}

class RotateArray extends RotateArraySolution {
  override def rotate(a: Array[Int], order: Int) = {
    // do not rotate redundantly if order is > a.length
    val curOrder = if (order > a.length) order % a.length else order

    for (i <- 0 until curOrder) {
      val t = a(i)
      a(i) = a(a.length - curOrder + i)
      a(a.length - curOrder + i) = t
    }
  }
}

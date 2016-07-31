package solution

import service.reflection.PositiveOutput
import tasktest.{RotateArraySolution, RotateArrayTest}

class TTRotateArray extends PositiveOutput {
  behavior of "ScalaTestRunner for RotateArray"

  override val suiteInstance = new RotateArrayTest(new RotateArray)
}

class RotateArray extends RotateArraySolution {
  override def rotate(a: Array[Int], steps: Int) = {
    // do not rotate redundantly if steps is > a.length
    val curSteps = if (steps > a.length) steps % a.length else steps

    for (i <- 0 until curSteps) {
      val t = a(i)
      a(i) = a(a.length - curSteps + i)
      a(a.length - curSteps + i) = t
    }
  }
}

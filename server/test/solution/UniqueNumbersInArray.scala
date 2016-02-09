package solution

import service.PositiveOutput
import tasktest.{UniqueNumbersInArraySolution, UniqueNumbersInArrayTest}

class TTUniqueNumbersInArray extends PositiveOutput {
  behavior of "ScalaTestRunner for UniqueNumbersInArray"

  override val suiteInstance = new UniqueNumbersInArrayTest(new UniqueNumbersInArray)
}


//solution
class UniqueNumbersInArray extends UniqueNumbersInArraySolution {
  override def uniqueNumbers(a: Array[Int]): Int = {
    val b = Array.fill(a.length)(true)
    var n = 0

    for (i <- a.indices) {
      if (b(i)) {
        n += 1
        for (j <- i until a.length) {
          b(j) = (a(j) != a(i)) && b(j)
        }
      }
    }

    n
  }
}

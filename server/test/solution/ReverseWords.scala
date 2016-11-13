package solution

import service.reflection.PositiveOutput
import tasktest.{ReverseWordsSolution, ReverseWordsTest}

class TTReverseWords extends PositiveOutput {
  behavior of "ScalaTestRunner for TTReverseWords"

  override val suiteInstance = new ReverseWordsTest(new ReverseWords)
}

class ReverseWords extends ReverseWordsSolution {
  override def reverse(s: String): String = {
    s.split(" ").reverse.filter(_.nonEmpty).mkString(" ")
  }
}

package solution

import service.reflection.PositiveOutput
import tasktest.{ReverseWordsSolution, ReverseWordsTest}

class TTReverseWords extends PositiveOutput {
  behavior of "ScalaTestRunner for TTReverseWords"

  override val suiteInstance = new ReverseWordsTest(new ReverseWords)
}

class ReverseWords extends ReverseWordsSolution {
  override def reverse(s: String): String = {
    val separator = " "
    val words = s.split(separator)
    words.reverse.filter(!_.isEmpty).mkString(separator)
  }
}

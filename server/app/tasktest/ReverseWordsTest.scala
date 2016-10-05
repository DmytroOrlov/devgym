package tasktest

import org.scalatest.{FlatSpec, Matchers}

class ReverseWordsTest(solution: ReverseWordsSolution) extends FlatSpec with Matchers {
  it should "reverse all words in the given string, but not the letters in the words" in {
    val reversed = solution.reverse("it is given string to be reversed")
    reversed should be("reversed be to string given is it")
  }

  it should "reverse string with redundant spaces by ignoring them" in {
    val reversed = solution.reverse("string   to    be  reversed")
    reversed should be("reversed be to string")
  }
}

trait ReverseWordsSolution {
  def reverse(s: String): String
}

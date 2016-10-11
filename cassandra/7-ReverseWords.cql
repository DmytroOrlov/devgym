INSERT INTO devgym.task (
  year,
  lang,
  timeuuid,
  name,
  description,
  solution_template,
  reference_solution,
  suite,
  solution_trait
) VALUES (
  '2016-01-01 00:00:00 UTC',
  'scalaLang',
  d09d48c4-4d2b-11e6-ac46-f58bd8d3766a,
  'Reverse words in a string',
  'Given an input string, reverse the string word by word.

   For example, given s = "it is given string to be reversed", return "reversed be to string given is it"',
  'class ReverseWords {
  def reverse(s: String): String = {
    
  }
}',
   'class ReverseWords extends ReverseWordsSolution {
      override def reverse(s: String): String = {
        val separator = " "
        val words = s.split(separator)
        words.reverse.filter(!_.isEmpty).mkString(separator)
      }
    }',
   'class ReverseWordsTest(solution: ReverseWordsSolution) extends FlatSpec with Matchers {
      it should "reverse all words in the given string, but not the letters in the words" in {
        val reversed = solution.reverse("it is given string to be reversed")
        reversed should be ("reversed be to string given is it")
      }

      it should "reverse string with redundant spaces by ignoring them" in {
        val reversed = solution.reverse("string   to    be  reversed")
        reversed should be ("reversed be to string")
      }
    }

    trait ReverseWordsSolution {
      def reverse(s: String): String
    }',
    'ReverseWordsSolution'
);

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
  94268893-4d29-11e6-ac46-f58bd8d3766a,
  'Rotate array by k steps forward',
  'Given array of integers rotate its content by k steps forward. For example: a = {1, 2, 3, 4, 5} to be rotated by 4 steps
  will result to a = {2, 3, 4, 5, 1}. If k = 5, then result would be: a = {1, 2, 3, 4, 5}, i.e. array rotated by its length,
  so that the elements took their previous places again. Note that algorithm should support k > a.length',
  'class RotateArray {
  def rotate(a: Array[Int], steps: Int) = {
    
  }
}',
   'class RotateArray {
      def rotate(a: Array[Int], steps: Int) = {
        // do not rotate redundantly if steps is > a.length
        val curSteps= if (steps > a.length) steps % a.length else steps

        for (i <- 0 until curSteps) {
          val t = a(i)
          a(i) = a(a.length - curSteps + i)
          a(a.length - curSteps + i) = t
        }
      }
    }',
   'class RotateArrayTest(solution: RotateArraySolution) extends FlatSpec with Matchers {
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
    }',
    'RotateArraySolution'
);

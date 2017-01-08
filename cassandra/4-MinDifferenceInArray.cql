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
  467530b0-e3b1-11e5-aa86-091830ac5256,
  'Min difference in array',
  'Find minimum difference in the given array. Numbers can be also negative. Minimum length of array is 2.
  For example, having such input Array(1, 4, 10, 22, 43, -12), then result should be 3.',
  'class MinDifferenceInArray {
  def minDifference(a: Array[Int]): Int = {
    
  }
}',
   'class MinDifferenceInArray {
      def minDifference(a: Array[Int]): Int = {
        val sa = a.sorted
        var diff = Math.abs(sa(0) - sa(1))

        for (i <- sa.init.indices) {
            val temp = Math.abs(sa(i) - sa(i + 1))
            if (temp < diff)
              diff = temp
        }
        diff
      }
    }',
   'class MinDifferenceInArrayTest(solution: MinDifferenceInArraySolution) extends FlatSpec with Matchers {

      it should "return 0 as min difference in array with non-unique numbers" in {
        solution.minDifference(Array(1, 2, 2, 2, 3, 12)) should be(0)
      }

      it should "return non 0 as min difference in array with unique numbers" in {
        solution.minDifference(Array(1, 4, 10, 22, 43, 12)) should be(2)
      }

      it should "return min difference in array with negative numbers" in {
        solution.minDifference(Array(1, 4, 10, 22, 43, -12)) should be(3)
      }
    }

    trait MinDifferenceInArraySolution {
      def minDifference(a: Array[Int]): Int
    }',
    'MinDifferenceInArraySolution'
);

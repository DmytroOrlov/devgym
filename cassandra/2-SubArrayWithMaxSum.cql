INSERT INTO devgym.task (
  year,
  type,
  timeuuid,
  name,
  description,
  solution_template,
  reference_solution,
  suite,
  solution_trait
) VALUES (
  '2016-01-01 00:00:00 UTC',
  'scalaClass',
  9894cd10-ce12-11e5-8ee9-091830ac5256,
  'Array of max sum',
  'Implement apply function to return  a sub-array of original array ''a'', which has maximum sum of its elements.
   For example, having such input Array(-2, 1, -3, 4, -1, 2, 1, -5, 4), then result should be Array(4, -1, 2, 1), which has maximum sum = 6. You can not rearrange elements of the initial array.',
   'class SubArrayWithMaxSum {
  def apply(a: Array[Int]): Array[Int] = {
        
  }
}',
   'class SubArrayWithMaxSum {
      def apply(a: Array[Int]): Array[Int] = {
        var currentSum = 0
        var maxSum = 0
        var left, right = 0
        var maxI = 0 //used when all negatives in the array

        for (i <- a.indices) {
          val incSum = currentSum + a(i)

          if (incSum > 0) {
            currentSum = incSum

            if (currentSum > maxSum) {
              maxSum = currentSum
              right = i
            }
          } else {
            left = i + 1
            right = left
            currentSum = 0
            if (a(i) > a(maxI)) maxI = i
          }
        }

        if (left == a.length) Array(a(maxI))
        else a.slice(left, right + 1)
      }
    }',
   'import org.scalatest.{FlatSpec, Matchers}

   class SubArrayWithMaxSumTest(solution: SubArrayWithMaxSumSolution) extends FlatSpec with Matchers {
      behavior of "SubArrayWithMaxSum"

      it should "return max sum sub array within given array" in {
        solution.apply(Array(-2, 1, -3, 4, -1, 2, 1, -5, 4)) should be(Array(4, -1, 2, 1))
        solution.apply(Array(-2, 1, -3, 4, -1, 2, 1, 5, 4)) should be(Array(4, -1, 2, 1, 5, 4))
        solution.apply(Array(2, -1, 0, 0, 0, 0, 1)) should be(Array(2))
      }

      it should "return the whole array when given array has only positive numbers" in {
        solution.apply(Array(2, 1, 3, 4, 1, 2, 1, 5, 4)) should be(Array(2, 1, 3, 4, 1, 2, 1, 5, 4))
      }

      it should "return max sum sub array when given array contains only negative numbers" in {
        solution.apply(Array(-2, -1, -3, -4, -1, -2, -1, -5, -4)) should be(Array(-1))
        solution.apply(Array(-2, -3, -3, -4, -6, -2, -6, -5, -1)) should be(Array(-1))
      }
    }

    trait SubArrayWithMaxSumSolution {
      def apply(a: Array[Int]): Array[Int]
    }',
    'SubArrayWithMaxSumSolution'
);

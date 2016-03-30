INSERT INTO devgym.task (
  year,
  type,
  timeuuid,
  name,
  description,
  solution_template,
  reference_solution,
  suite
) VALUES (
  '2016-01-01 00:00:00 UTC',
  'scalaClass',
  3475a7b0-e56d-11e5-aa86-091830ac5256,
  'Max occurrence in array',
  'Find maximum number of occurrences in the given array. Return the element which has the max number of occurrences than
  other elements in the input array. For example, array of (1, 2, 2, 4, 4, 4, 1) should result the element 4, as it has 3
  occurrences in total. In case of equal number of occurrences return any of the elements.',
  'class MaxOccurrenceInArray {
     def maxOccurrence(uniqueNum: Int, a: Array[Int]): Int = {

     }
   }',
   'class MaxOccurrenceInArray {
      def maxOccurrence(uniqueNum: Int, a: Array[Int]): Int = {
        val count = new Array[Int](uniqueNum + 1)
        var maxOccurrence = 1
        var index = 0

        for (i <- a.indices) {
          if (count(a(i)) > 0) {
            val tmp = count(a(i))
            if (tmp > maxOccurrence) {
              maxOccurrence = tmp
              index = i
            }
            count(a(i)) = tmp + 1
          } else {
            count(a(i)) = 1
          }
        }

        a(index)
      }
    }',
   'class MaxOccurrenceInArrayTest(solution: MaxOccurrenceInArraySolution) extends FlatSpec with Matchers {
      it should "return max occurrence in array" in {
        solution.maxOccurrence(3, Array[Int](1, 2, 2, 3, 3, 3, 1)) should be(3)
        solution.maxOccurrence(3, Array[Int](1, 1, 1, 2, 2, 3, 3)) should be(1)
      }

      it should "return max occurrence in array of 1 element" in {
        solution.maxOccurrence(1, Array(1)) should be(1)
      }

      it should "return max occurrence in array of equal count of max occurrence" in {
        solution.maxOccurrence(3, Array(1, 2, 2, 3, 3)) ensuring (m => m == 2 || m == 3) //improve
      }
    }

    trait MaxOccurrenceInArraySolution {
      def maxOccurrence(M: Int, A: Array[Int]): Int
    }'
);

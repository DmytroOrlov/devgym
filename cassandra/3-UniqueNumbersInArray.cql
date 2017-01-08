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
  915b6150-d90f-11e5-aa5b-091830ac5256,
  'Unique numbers in Array',
  'Find number of unique elements in array of integers. For example, array of (4, 2, 2, 32, 3, 32, 2) should result
  the 4 as number of unique elements in the array.',
  'class UniqueNumbersInArray {
  def uniqueNumbers(a: Array[Int]): Int = {
    
  }
}',
   'class UniqueNumbersInArray {
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
    }',
   'import org.scalatest.{FlatSpec, Matchers}

    class UniqueNumbersInArrayTest(solution: UniqueNumbersInArraySolution) extends FlatSpec with Matchers {
      behavior of "UniqueNumbersInArray"

      it should "return count of unique numbers in array " in {
        solution.uniqueNumbers(Array(4, 2, 2, 32, 3, 32, 2)) should be(4)
      }

      it should "return count of unique numbers in array of equal numbers " in {
        solution.uniqueNumbers(Array(2, 2, 2, 2, 2)) should be(1)
      }

      it should "return count equals array length when all numbers are unique " in {
        solution.uniqueNumbers(Array(2, 1, 3, 4, -4)) should be(5)
      }
    }

    trait UniqueNumbersInArraySolution {
      def uniqueNumbers(a: Array[Int]): Int
    }',
    'UniqueNumbersInArraySolution'
);

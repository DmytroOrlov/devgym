package service.reflection

import monix.execution.Scheduler.Implicits.global
import org.scalameter.{Key, Warmer, _}
import org.scalatest.{FlatSpec, Matchers, Suite}
import service.StringBuilderRunner
import shared.model.TestStatus
import tag.PerfTests

import scala.language.reflectiveCalls
import scala.util.Try

class RunnerPerformanceTest extends FlatSpec with Matchers {
  val standardConfig = config(
    Key.exec.minWarmupRuns -> 10,
    Key.exec.maxWarmupRuns -> 40,
    Key.exec.benchRuns -> 50,
    Key.verbose -> true
  ) withWarmer new Warmer.Default

  val solution =
    """   class MaxOccurrenceInArray {
            def maxOccurrence(uniqueNum: Int, a: Array[Int]): Int = {
              val count = new Array[Int](uniqueNum + 1)
              var maxOccurrence = 1
              var index = 0

              for (i <- a.indices) {
              if (count(a(i)) > 0) {
                val tmp = count(a(i)) + 1
                if (tmp > maxOccurrence) {
                  maxOccurrence = tmp
                  index = i
                }
                  count(a(i)) = tmp
                } else {
                  count(a(i)) = 1
                }
              }

              a(index)
            }
          }""".stripMargin

  val suiteWithoutSolutionTrait =
    """   class MaxOccurrenceInArrayTest[A <: {def maxOccurrence(m: Int, ar: Array[Int]): Int}](solution: A) extends FlatSpec with Matchers {
            it should "return max occurrence in array" in {
              solution.maxOccurrence(3, Array[Int](1, 2, 2, 3, 3, 3, 1)) should be(3)
              solution.maxOccurrence(3, Array[Int](1, 1, 1, 2, 2, 3, 3)) should be(1)
            }

            it should "return max occurrence in array of 1 element" in {
              solution.maxOccurrence(1, Array(1)) should be(1)
            }

            it should "return max occurrence in array of equal count of max occurrence" in {
              solution.maxOccurrence(3, Array(1, 2, 2, 3, 3)) should (be(2) or be(3))
            }
          }""".stripMargin

  val suite =
    """
      class MaxOccurrenceInArrayTest(solution: MaxOccurrenceInArraySolution) extends FlatSpec with Matchers {
        it should "return max occurrence in array" in {
          solution.maxOccurrence(3, Array[Int](1, 2, 2, 3, 3, 3, 1)) should be(3)
          solution.maxOccurrence(3, Array[Int](1, 1, 1, 2, 2, 3, 3)) should be(1)
        }

        it should "return max occurrence in array of 1 element" in {
          solution.maxOccurrence(1, Array(1)) should be(1)
        }

        it should "return max occurrence in array of equal count of max occurrence" in {
          solution.maxOccurrence(3, Array(1, 2, 2, 3, 3)) should (be(2) or be(3))
        }
      }

      trait MaxOccurrenceInArraySolution {
        def maxOccurrence(M: Int, A: Array[Int]): Int
      }
    """.stripMargin

  def runPerformanceTest(executor: ((String) => Unit) => String, testName: String): Unit = {
    def testReport(executor: ((String) => Unit) => Unit) = Try(StringBuilderRunner(executor))

    //when
    val result = service.testResult(testReport(executor))
    println(s"${result.status} ${result.errorMessage}")
    //then
    result.testStatus should be(TestStatus.Passed)

    //when
    val time = standardConfig measure testReport(executor)
    println(s"$testName: time = $time")
    //then
    time.value.asInstanceOf[Double] should be <= 1000d
  }

  it should "execute test suite without solution trait within 1 second" taggedAs PerfTests in new ScalaDynamicNoTraitRunner {
    runPerformanceTest(executor = execSuiteNoTrait(solution, suiteWithoutSolutionTrait), "ScalaTest dynamic, not trait")
  }

  it should "execute test suite with solution trait within 1 second" taggedAs PerfTests in {
    val dynamicRunnerWithTrait = new ScalaDynamicRunner() {}
    runPerformanceTest(executor = dynamicRunnerWithTrait(solution, suite, "MaxOccurrenceInArraySolution"),
      "ScalaTest dynamic with trait")
  }

  it should "execute test suite with suite class and solution trait from classpath within 1 second" taggedAs PerfTests in {
    val runtimeRunner = new ScalaRuntimeRunner() {}
    runPerformanceTest(executor = runtimeRunner(
      Class.forName("tasktest.MaxOccurrenceInArrayTest").asInstanceOf[Class[Suite]],
      Class.forName("tasktest.MaxOccurrenceInArraySolution").asInstanceOf[Class[AnyRef]],
      solution
    ), "ScalaTest runtime suite with trait")
  }
}
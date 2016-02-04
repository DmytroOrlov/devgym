package service

import org.scalatest._
import service.ScalaTestRunnerTest._

class ScalaTestDynamicRunnerTest extends FlatSpec with Matchers {
  behavior of "ScalaTestRunner for dynamic solution and suite code"

  val correctSuite =
    """class SleepInTest(solution: SleepInSolution) extends FlatSpec with Matchers {
      behavior of "SleepIn"

      it should "sleepIn when it is not a weekday and it is not a vacation" in {
          solution.sleepIn(false, false) shouldBe true
      }

      it should "not sleepIn when it is a weekday and it is not a vacation" in {
          solution.sleepIn(true, false) shouldBe false
      }

      it should "sleepIn when it is not a weekday and it is a vacation" in {
        solution.sleepIn(false, true) shouldBe true
      }
      }

      trait SleepInSolution {
        def sleepIn(weekday: Boolean, vacation: Boolean): Boolean
      }
    """.stripMargin

  val noSuiteName =
      """class (solution: SleepInSolution) extends FlatSpec with Matchers {
          behavior of "SleepIn"

          it should "sleepIn when it is not a weekday and it is not a vacation" in {
            solution.sleepIn(false, false) shouldBe true
          }""".stripMargin

  val noTraitName =
      """class SleepInTest(solution: SleepInSolution) extends FlatSpec with Matchers {
          behavior of "SleepIn"

          it should "sleepIn when it is not a weekday and it is not a vacation" in {
            solution.sleepIn(false, false) shouldBe true
          }""".stripMargin


  it should "throw RuntimeException when suite does not have a class name" in {
    intercept[RuntimeException] {
      ScalaTestRunner.execSuite(correctSolution, noSuiteName)
    }
  }

  it should "throw RuntimeException when suite does not have a trait type for contructor" in {
    intercept[RuntimeException] {
      ScalaTestRunner.execSuite(correctSolution, noTraitName)
    }
  }

  it should "not return failed status when correct solution is provided" in {
    val report = ScalaTestRunner.execSuite(correctSolution, correctSuite)
    report shouldNot (be(empty) and include regex ScalaTestRunner.failedMarker)
  }
}

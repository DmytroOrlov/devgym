package service

import org.scalatest.{FlatSpec, Matchers}
import service.ScalaTestRunner.SolutionException
import service.ScalaTestRunnerTest._

class ScalaTestDynamicRunnerTest extends FlatSpec with Matchers {
  behavior of "ScalaTestRunner for dynamic solution and suite code"

  val correctSuite =
    """class SleepInTest[A <: {def sleepIn(weekday : Boolean, vacation : Boolean) : Boolean}](solution: A) extends FlatSpec with Matchers {
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
    """.stripMargin

  val noSuiteName =
    """class [A <: {def sleepIn(weekday : Boolean, vacation : Boolean) : Boolean}](solution: A) extends FlatSpec with Matchers {
          behavior of "SleepIn"

          it should "sleepIn when it is not a weekday and it is not a vacation" in {
            solution.sleepIn(false, false) shouldBe true
          }""".stripMargin

  it should "throw SolutionException when suite does not have a class name" in {
    intercept[SolutionException] {
      ScalaTestRunner.execSuite(correctSolution, noSuiteName)
    }
  }

  it should "not return failed status when correct solution is provided" in {
    val report = ScalaTestRunner.execSuite(correctSolution, correctSuite)
    report shouldNot (be(empty) and include regex ScalaTestRunner.failedMarker)
  }
}

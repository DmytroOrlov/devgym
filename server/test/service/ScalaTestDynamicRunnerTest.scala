package service

import monifu.concurrent.Implicits.globalScheduler
import org.scalatest.FlatSpecLike

import scala.util.Try

class ScalaTestDynamicRunnerTest extends ScalaTestRunnerTest with FlatSpecLike {
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

  val runner = new ScalaDynamicRunner() {}

  override def getReport(solution: String, check: Boolean = false) = {
    val unchecked = Try(StringBuilderRunner(runner(solution, correctSuite)))
    if (check) unchecked.check
    else unchecked
  }

  it should "return failure when suite does not have a class name" in {
    Try(StringBuilderRunner(runner(correctSolution, noSuiteName))).isFailure shouldBe true
  }

  it should "return failure when suite does not have a trait type for contructor" in {
    Try(StringBuilderRunner(runner(correctSolution, noTraitName))).isFailure shouldBe true
  }
}

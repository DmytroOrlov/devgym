package service.reflection

import monix.execution.Scheduler.Implicits.global
import org.scalatest.FlatSpecLike
import service.StringBuilderRunner

import scala.util.Try

class DynamicRunnerTest extends RunnerTest with FlatSpecLike {
  behavior of "ScalaTestRunner for dynamic solution and suite code"

  val solutionTrait = "SleepInSolution"

  val correctSuite =
    s"""class SleepInTest(solution: SleepInSolution) extends FlatSpec with Matchers {
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

      trait $solutionTrait {
        def sleepIn(weekday: Boolean, vacation: Boolean): Boolean
      }
    """.stripMargin

  val noSuiteName =
    s"""class (solution: $solutionTrait) extends FlatSpec with Matchers {
          behavior of "SleepIn"

          it should "sleepIn when it is not a weekday and it is not a vacation" in {
            solution.sleepIn(false, false) shouldBe true
          }""".stripMargin

  val noTraitName =
    s"""class SleepInTest(solution: $solutionTrait) extends FlatSpec with Matchers {
          behavior of "SleepIn"

          it should "sleepIn when it is not a weekday and it is not a vacation" in {
            solution.sleepIn(false, false) shouldBe true
          }""".stripMargin

  val runner = new ScalaDynamicRunner()

  it should "return failure when suite does not have a class name" in {
    Try(StringBuilderRunner(runner(correctSolution, noSuiteName, solutionTrait))).isFailure shouldBe true
  }

  it should "return failure when suite does not have a trait type for contructor" in {
    Try(StringBuilderRunner(runner(correctSolution, noTraitName, solutionTrait))).isFailure shouldBe true
  }
}

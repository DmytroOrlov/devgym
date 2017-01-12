package service.reflection

import monix.execution.Scheduler.Implicits.global
import org.scalatest.{FlatSpec, Matchers}
import service.StringBuilderRunner

import scala.util.Try

class NoTraitDynamicRunnerTest extends FlatSpec with Matchers with CorrectSolution {
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

  it should "retrun failure when suite does not have a class name" in new ScalaDynamicNoTraitRunner {
    Try(StringBuilderRunner(execSuiteNoTrait(correctSolution, noSuiteName))).isFailure shouldBe true
  }

  it should "return success when correct solution is provided" in new ScalaDynamicNoTraitRunner {
    private val res: Try[String] = Try(StringBuilderRunner(execSuiteNoTrait(correctSolution, correctSuite)))
    res.failed.foreach(t => println(t))
    res.isSuccess shouldBe true
  }
}

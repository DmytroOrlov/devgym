package service.reflection

import monix.execution.Scheduler.Implicits.global
import org.scalatest.{FlatSpec, Matchers}
import service.StringBuilderRunner

import scala.util.Try

class DynamicRunnerSecurityTest extends FlatSpec with Matchers {

  val runner = new ScalaDynamicRunner() {}

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

  private def getSolution(clazz: String) =
    s"""class A {
            def sleepIn(weekday: Boolean, vacation: Boolean): Boolean = {
              import $clazz
              !weekday || vacation
            }
          }"""

  it should "return failure when solution imports disables classes" in {
    TaskClassLoader.forbiddenClasses.forall(c => {
      val solution = getSolution(c)
      val result = Try(StringBuilderRunner(runner(solution, correctSuite, solutionTrait)))
      result.failed.foreach(t => println(t))
      result.isFailure
    }) shouldBe true
  }
}

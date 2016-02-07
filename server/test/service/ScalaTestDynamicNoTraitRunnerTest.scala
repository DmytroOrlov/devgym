package service

import monifu.concurrent.Implicits.globalScheduler
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.time.Span._
import org.scalatest.{FlatSpec, Matchers}

import scala.concurrent.duration._

class ScalaTestDynamicNoTraitRunnerTest extends FlatSpec with Matchers with ScalaTestCorrectSolution with ScalaFutures {
  behavior of "ScalaTestRunner for dynamic solution and suite code"

  implicit val c = PatienceConfig(10.seconds)

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

  it should "retrun failure when suite does not have a class name" in new ScalaTestRunner {
    execSuiteNoTrait(correctSolution, noSuiteName).future.failed.futureValue
  }

  it should "return success when correct solution is provided" in new ScalaTestRunner {
    execSuiteNoTrait(correctSolution, correctSuite).future.futureValue
  }
}

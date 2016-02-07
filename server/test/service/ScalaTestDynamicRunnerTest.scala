package service

import monifu.concurrent.Implicits.globalScheduler
import org.scalatest.FlatSpecLike

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

  val r = new ScalaTestRunner()

  override def getReport(solution: String, checked: Boolean = false) = {
    val unchecked = r(solution, correctSuite)
    if (checked) r.check(unchecked)
    else unchecked
  }

  it should "return failure when suite does not have a class name" in new ScalaTestRunner {
    apply(correctSolution, noSuiteName)._2.failed.futureValue
  }

  it should "return failure when suite does not have a trait type for contructor" in new ScalaTestRunner {
    apply(correctSolution, noTraitName)._2.failed.futureValue
  }
}

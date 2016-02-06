package service

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


  override def getReport(solution: String, checked: Boolean = false): Try[String] = new ScalaTestRunner().apply(solution, correctSuite, checked)

  it should "return failure when suite does not have a class name" in new ScalaTestRunner {
    apply(correctSolution, noSuiteName, checked = false).isFailure shouldBe true
  }

  it should "return failure when suite does not have a trait type for contructor" in new ScalaTestRunner {
    apply(correctSolution, noTraitName, checked = false).isFailure shouldBe true
  }
}

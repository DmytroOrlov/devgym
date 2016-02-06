package service

import org.scalatest.{FlatSpec, Matchers, Suite}

class ScalaTestRunnerTest extends FlatSpec with Matchers with ScalaTestCorrectSolution {
  behavior of "ScalaTestRunner"
  val incorrectSolution = "class A { def sleepIn(weekday: Boolean, vacation: Boolean): Boolean = {weekday || vacation}}"

  it should "return success when correct solution is provided" in {
    val report = getReport(correctSolution)
    report.isSuccess shouldBe true
  }

  it should "return success when compilable solution is provided" in {
    val report = getReport(incorrectSolution)
    report.isSuccess shouldBe true
  }

  it should "return failure when solution is not compilable" in {
    val report = getReport("/")
    report.isFailure shouldBe true
  }

  def runner() = new ScalaTestRunner().apply(
    Class.forName("service.SleepInTest").asInstanceOf[Class[Suite]],
    Class.forName("service.SleepInSolution").asInstanceOf[Class[AnyRef]]
  ) _

  def getReport(solution: String) = runner().apply(solution)
}

trait ScalaTestCorrectSolution {
  val correctSolution = "class A { def sleepIn(weekday: Boolean, vacation: Boolean): Boolean = {!weekday || vacation}}"
}

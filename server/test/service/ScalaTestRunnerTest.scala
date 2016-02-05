package service

import org.scalatest.{FlatSpec, Matchers, Suite}

class ScalaTestRunnerTest extends FlatSpec with Matchers with ScalaTestCorrectSolution {
  behavior of "ScalaTestRunner"
  val incorrectSolution = "class A { def sleepIn(weekday: Boolean, vacation: Boolean): Boolean = {weekday || vacation}}"

  it should "not return failed status when correct solution is provided" in {
    val report = getReport(correctSolution)
    report shouldNot (be(empty) and include regex ScalaTestRunner.failedMarker)
  }

  it should "return failed status when incorrect solution is provided" in {
    val report = getReport(incorrectSolution)
    report should (not be empty and include regex ScalaTestRunner.failedMarker)
  }

  it should "return failed status when solution is not compilable" in {
    val report = getReport("/")
    report should (not be empty and include regex ScalaTestRunner.failedInRuntimeMarker)
  }

  def getReport(solution: String) = {
    ScalaTestRunner.tryExecSuite(
      solution,
      Class.forName("service.SleepInTest").asInstanceOf[Class[Suite]],
      Class.forName("service.SleepInSolution").asInstanceOf[Class[AnyRef]]
    )
  }
}

trait ScalaTestCorrectSolution {
  val correctSolution = "class A { def sleepIn(weekday: Boolean, vacation: Boolean): Boolean = {!weekday || vacation}}"
}

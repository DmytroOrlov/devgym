package service

import org.scalatest.{FlatSpec, Matchers, Suite}

class ScalaTestRunnerTest extends FlatSpec with Matchers {
  behavior of "ScalaTestRunner"

  it should "not return failed status when correct solution is provided" in {
    val correctSolution = "class A { def sleepIn(weekday: Boolean, vacation: Boolean): Boolean = {!weekday || vacation}}"
    val report = getReport(correctSolution)
    report shouldNot (be(empty) and include regex ScalaTestRunner.failedMarker)
  }

  it should "return failed status when incorrect solution is provided" in {
    val incorrectSolution = "class A { def sleepIn(weekday: Boolean, vacation: Boolean): Boolean = {weekday || vacation}}"
    val report = getReport(incorrectSolution)
    report should (not be empty and include regex ScalaTestRunner.failedMarker)
  }

  it should "return failed status when solution is not compilable" in {
    val report = getReport("/")
    report should (not be empty and include regex ScalaTestRunner.failedInRuntimeMarker)
  }

  def getReport(solution: String) = {
    ScalaTestRunner.execSuite(
      solution,
      Class.forName("tasktest.SleepInTest").asInstanceOf[Class[Suite]],
      Class.forName("tasktest.SleepInSolution").asInstanceOf[Class[AnyRef]]
    )
  }
}

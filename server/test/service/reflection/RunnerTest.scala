package service.reflection

import monix.execution.Scheduler.Implicits.global
import org.scalatest.{FlatSpec, Matchers, Suite}
import service._
import shared.model.{Event, TestResult, TestStatus}

import scala.util.Try

class RunnerTest extends FlatSpec with Matchers with CorrectSolution {
  behavior of "ScalaTestRunner"
  val incorrectSolution = "class A { def sleepIn(weekday: Boolean, vacation: Boolean): Boolean = {weekday || vacation}}"

  it should "return success when correct solution is provided" in {
    val report = getReport(correctSolution)
    report.testStatus shouldBe TestStatus.Passed
  }

  it should "return success when compilable solution is provided" in {
    getReport(incorrectSolution).testStatus shouldBe TestStatus.FailedByTest
  }

  it should "return failure when check compilable but wrong solution" in {
    getReport(incorrectSolution).testStatus shouldBe TestStatus.FailedByTest
  }

  it should "return failure when solution is not compilable" in {
    getReport("/").testStatus shouldBe TestStatus.FailedByCompilation
  }

  private val runner = new ScalaRuntimeRunner()

  private def getReport(solution: String, check: Boolean = false): TestResult = {
    val block: (String => Unit) => Unit = runner(
      Class.forName("service.SleepInTest").asInstanceOf[Class[Suite]],
      Class.forName("service.SleepInSolution").asInstanceOf[Class[AnyRef]],
      solution)
    val (checkNext, getTestResult) = testSync
    getTestResult(block(checkNext)) match {
      case result: TestResult => result
      case _ => fail()
    }
  }
}

trait CorrectSolution {
  val correctSolution = "class A { def sleepIn(weekday: Boolean, vacation: Boolean): Boolean = {!weekday || vacation}}"
}

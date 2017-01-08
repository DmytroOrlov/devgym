import monix.execution.cancelables.BooleanCancelable
import shared.model.{Event, TestResult, TestStatus}
import shared.view.SuiteReportUtil.red

import scala.util.{Failure, Success, Try}

package object service {
  //scalatest keyword
  val testFailedMarker = "FAILED"

  implicit class RichRunnerString(val output: Try[String]) extends AnyVal {
    def check = output.filter(!_.contains(testFailedMarker))
  }

  type CheckNext = (String) => Unit

  type OnBlockComplete = (Try[Unit]) => Unit

  type PushResult = (Event) => Unit

  def test(pushResult: PushResult): (CheckNext, OnBlockComplete) = {
    def isFailed(line: String): Boolean = line.startsWith(red) && line.contains("*** FAILED ***")

    def testResult(blockRes: Try[Unit], failedByTest: => Boolean): TestResult = blockRes match {
      case Success(_) if failedByTest => TestResult(TestStatus.FailedByTest.toString)
      case Success(_) => TestResult(TestStatus.Passed.toString)
      case Failure(e) => TestResult(TestStatus.FailedByCompilation.toString, e.getMessage)
    }

    val cancelable = BooleanCancelable()

    val checkNext = (line: String) => if (isFailed(line)) cancelable.cancel()
    val onBlockComplete = (blockRes: Try[Unit]) => {
      val res = testResult(blockRes, failedByTest = cancelable.isCanceled)
      pushResult(res)
    }
    checkNext -> onBlockComplete
  }
}

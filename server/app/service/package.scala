import service.reflection.TaskSecurityManager
import shared.model.{TestResult, TestStatus}

import scala.util.{Failure, Success, Try}

package object service {
  //TODO: check predefined SecurityManager before setup
  System.setSecurityManager(new TaskSecurityManager())

  //scalatest keyword
  val testFailedMarker = "FAILED"

  implicit class RichRunnerString(val output: Try[String]) extends AnyVal {
    def check = output.filter(!_.contains(testFailedMarker))
  }

  def testResult(report: Try[String]): TestResult =
    report match {
      case Success(s) =>
        val (status, error) =
        //no need to send test result as error, even it is failed test.
        //Improvement here is to introduce new TestStatus like FailedByTest, FailedByCompilation instead of ambiguous Failed
          if (s.contains(testFailedMarker)) (TestStatus.Failed.toString, "")
          else (TestStatus.Passed.toString, "")
        TestResult(status, error)
      case Failure(e) => TestResult(TestStatus.Failed.toString, e.getMessage)
    }
}

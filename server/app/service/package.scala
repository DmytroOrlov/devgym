import shared.model.{TestResult, TestStatus}

import scala.util.{Failure, Success, Try}

package object service {
  //scalatest keyword
  val failed = "FAILED"

  implicit class RichRunnerString(val output: Try[String]) extends AnyVal {
    def check = output.filter(!_.contains(failed))
  }

  def testResult(report: Try[String]): TestResult =
    report match {
      case Success(s) =>
        val (status, error) =
        //no need to send test result as error, even it is test is failed.
        //Improvement here is to introduce new TestStatus like FailedByTest, FailedByCompilation instead of ambiguous Failed
          if (s.contains(failed)) (TestStatus.Failed.toString, "")
          else (TestStatus.Passed.toString, "")
        TestResult(status, error)
      case Failure(e) => TestResult(TestStatus.Failed.toString, e.getMessage)
    }
}

import shared.model.{TestResult, TestStatus}

import scala.util.{Failure, Success, Try}

package object service {
  //scalatest keyword
  val testFailedMarker = "FAILED"

  implicit class RichRunnerString(val output: Try[String]) extends AnyVal {
    def check = output.filter(!_.contains(testFailedMarker))
  }

  def testResult(report: Try[String]): TestResult =
    report match {
      case Success(s) =>
        val status =
          if (s.contains(testFailedMarker)) TestStatus.FailedByTest
          else TestStatus.Passed
        TestResult(status.toString)
      case Failure(e) =>
        TestResult(TestStatus.FailedByCompilation.toString, e.getMessage)
    }
}

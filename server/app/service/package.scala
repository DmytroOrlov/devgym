import service.reflection.SuiteException
import shared.model.{TestResult, TestStatus}

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}

package object service {
  //scalatest keyword
  val failed = "FAILED"

  implicit class RichRunnerFuture(val future: Future[String]) extends AnyVal {
    def check(implicit ec: ExecutionContext) =
      future.flatMap(r => if (r.contains(failed)) Future.failed(SuiteException(r)) else Future.successful(r))
  }

  implicit class RichRunnerString(val output: Try[String]) extends AnyVal {
    def check = output.filter(!_.contains(failed))
  }

  def testStatus(report: Try[String]): Option[TestResult] =
    Option(report match {
      case Success(s) =>
        val (status, error) =
          if (s.contains(failed)) (TestStatus.Failed.toString, s)
          else (TestStatus.Passed.toString, "")
        TestResult(status, error)
      case Failure(e) => TestResult(TestStatus.Failed.toString, e.getMessage)
    })
}

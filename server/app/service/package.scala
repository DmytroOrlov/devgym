import service.reflection.SuiteException

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try

package object service {
  val failed = "FAILED" //scalatest keyword

  implicit class RichRunnerFuture(val future: Future[String]) extends AnyVal {
    def check(implicit ec: ExecutionContext) =
      future.flatMap(r => if (r.contains(failed)) Future.failed(SuiteException(r)) else Future.successful(r))
  }

  implicit class RichRunnerString(val output: Try[String]) extends AnyVal {
    def check = output.filter(!_.contains(failed))
  }

  def testStatus(report: Either[Throwable, String]): Option[String] = {
    Option(report.fold(
      f => f.getMessage,
      s => if (s.contains(failed)) "Test Failed" else "Test Passed"
    ))
  }
}

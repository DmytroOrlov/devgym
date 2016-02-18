import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try

package object service {
  val failed = "FAILED"

  implicit class RichRunnerFuture(val future: Future[String]) extends AnyVal {
    def check(implicit ec: ExecutionContext) =
      future.flatMap(r => if (r.contains(failed)) Future.failed(new SuiteException(r)) else Future.successful(r))
  }

  implicit class RichRunnerString(val output: Try[String]) extends AnyVal {
    def check = output.filter(!_.contains(failed))
  }

}

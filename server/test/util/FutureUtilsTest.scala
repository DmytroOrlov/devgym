package util

import java.util.concurrent.Callable
import java.util.concurrent.Executors.newFixedThreadPool

import com.google.common.util.concurrent.MoreExecutors.listeningDecorator
import org.scalamock.scalatest.MockFactory
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{MustMatchers, WordSpec}
import util.FutureUtils.toFuture

import scala.concurrent.ExecutionContext.Implicits.global

class FutureUtilsTest extends WordSpec with MockFactory with ScalaFutures with MustMatchers {
  val service = listeningDecorator(newFixedThreadPool(10))

  "toFuture of GuavaFuture" when {
    "success" should {
      "complete scala future" in {
        val result = Explosion(boom = true)

        val futureResult = mockFunction[Explosion]
        futureResult expects() returning result

        val explosion = toFuture(service.submit(new Callable[Explosion]() {
          override def call() = futureResult()
        }))
        whenReady(explosion) {
          _ mustBe result
        }
      }
    }
    "failure" should {
      "fail scala future" in {
        val resultException = mockFunction[Explosion]
        resultException expects() throws new RuntimeException("epic fail")

        val explosion = toFuture(service.submit(new Callable[Explosion]() {
          override def call() = resultException()
        }))
        whenReady(explosion.failed) {
          _ mustBe a[Exception]
        }
      }
    }
  }

  case class Explosion(boom: Boolean)

  case class FutureException(msg: String) extends RuntimeException(msg)

}

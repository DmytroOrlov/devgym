package util

import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{Matchers, WordSpec}

import scala.concurrent.Future

class TryFutureTest extends WordSpec with Matchers with ScalaFutures {

  "TryFuture(block: => Future[A])" when {

    "block throw exception" should {
      "make Future.failed" in {
        val exception = new TestException
        val f = TryFuture({ throw exception; Future.successful("unreal result")})

        whenReady(f.failed) {
          _ shouldBe a[TestException]
        }
      }
    }
    "block Success with future result" should {
      "return it" in {
        val f = TryFuture(Future.successful("result"))

        whenReady(f) {
          _ shouldBe "result"
        }
      }
    }
    "block Success but Future.failed" should {
      "return future exception" in {
        val exception = new TestException
        val f = TryFuture(Future.failed(exception))

        whenReady(f.failed) {
          _ shouldBe a[TestException]
        }
      }
    }
  }

  final class TestException extends RuntimeException

}

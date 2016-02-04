package controllers

import controllers.ControllerTest._
import org.scalamock.scalatest.MockFactory
import org.scalatestplus.play.PlaySpec
import play.api.test.Helpers._
import play.api.test._

class ControllerTest extends PlaySpec with MockFactory {
  "Application controller" when {
    "get request to index" should {
      "result with OK" in withController { controller =>
        val result = controller.index(FakeRequest())
        status(result) mustBe OK
        contentAsString(result) must (include("/task") and include("/addTask") and include("/register"))
      }
    }
    "get request for logout" should {
      "redirect" in withController { controller =>
        val result = controller.logout(FakeRequest())
        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some("/")
      }
    }
  }
}

object ControllerTest {
  def withController[T](block: Application => T): T = {
    block(new Application(new MockMessageApi))
  }
}

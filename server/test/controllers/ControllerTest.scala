package controllers

import controllers.ControllerTest._
import org.scalamock.scalatest.MockFactory
import org.scalatestplus.play.PlaySpec
import play.api.test.Helpers._
import play.api.test._

import scala.concurrent.ExecutionContext.Implicits.global

class ControllerTest extends PlaySpec with MockFactory {
  "Application controller" when {
    "get request to index" should {
      "result with OK" in withAppController { controller =>
        val result = controller.index(FakeRequest())
        status(result) mustBe OK
        contentAsString(result) must (include("/task") and include("/addTask") and include("/register"))
      }
    }
    "get request for logout" should {
      "redirect" in withAppController { controller =>
        val result = controller.logout(FakeRequest())
        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some("/")
      }
    }
  }
  "NewTask controller" when {
    "get addTask" should {
      "redirect" in withNewTaskController { controller =>
        val result = controller.getAddTask(FakeRequest())
        status(result) mustBe OK
        contentAsString(result) must (include("<form") and include("/addTask"))
      }
    }
  }
}

object ControllerTest {
  def withAppController[T](block: Application => T): T = {
    block(new Application(new MockMessageApi))
  }

  def withNewTaskController[T](block: NewTask => T): T = {
    block(new NewTask(null, new MockMessageApi))
  }
}

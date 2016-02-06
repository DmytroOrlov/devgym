package controllers

import controllers.ControllerTest._
import org.scalatestplus.play.PlaySpec
import play.api.test.Helpers._
import play.api.test._

import scala.concurrent.ExecutionContext.Implicits.global

class ControllerTest extends PlaySpec {

  "NewTask controller" when {
    "get addTask" should {
      "result with form" in withNewTaskController { controller =>
        val result = controller.getAddTask(FakeRequest())
        status(result) mustBe OK
        contentAsString(result) must (include("<form") and include("/addTask"))
      }
    }
    "post no form to addTask" should {
      "result BadRequest with error" in withNewTaskController { controller =>
        val result = controller.postNewTask(FakeRequest())
        status(result) mustBe BAD_REQUEST
        contentAsString(result) must (include("<form") and include("/addTask") and include("error"))
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

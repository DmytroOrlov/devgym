package controllers

import controllers.ControllerTest._
import org.scalatestplus.play.PlaySpec
import play.api.test.Helpers._
import play.api.test._

import scala.concurrent.ExecutionContext.Implicits.global

class ControllerTest extends PlaySpec {

  "NewTask controller" when {
    "post no form to addTask" should {
      "result BadRequest with error" in withNewTaskController { controller =>
        val result = controller.postNewTask(FakeRequest("POST", "ignore").withTextBody("taskDescription=1&solutionTemplate=2&referenceSolution=3&suite=4"))
        status(result) mustBe BAD_REQUEST
//        contentAsString(result) must include("123")
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

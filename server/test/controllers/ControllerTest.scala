package controllers

import controllers.ControllerTest._
import dal.Dao
import models.TaskType.scalaClass
import models.{TaskType, Task}
import org.scalamock.scalatest.MockFactory
import org.scalatestplus.play.PlaySpec
import play.api.test.Helpers._
import play.api.test._
import service.ScalaTestRunnerContract

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class ControllerTest extends PlaySpec with MockFactory {

  "NewTask controller" when {
    "post fail with scalaTestRunner when addTask" should {
      "result BadRequest with error" in {
        val scalaTestRunner = mock[ScalaTestRunnerContract]
        scalaTestRunner.execSuite _ expects("3", "4") throws new RuntimeException
        withNewTaskController(null, scalaTestRunner) { controller =>
          val result = controller.postNewTask(FakeRequest("POST", "ignore").withFormUrlEncodedBody("taskDescription" -> "1", "solutionTemplate" -> "2", "referenceSolution" -> "3", "suite" -> "4"))
          status(result) mustBe BAD_REQUEST
          contentAsString(result) must include("""class="error"></dd>""")
        }
      }
    }
    "post fail with dao when addTask" should {
      "result BadRequest with error" in {
        val scalaTestRunner = mock[ScalaTestRunnerContract]
        scalaTestRunner.execSuite _ expects("3", "4") returns "passed"
        val dao = mock[Dao]
        dao.addTask _ expects Task(scalaClass, "1", "2", "3", "4") returns Future.failed(new RuntimeException)
        withNewTaskController(dao, scalaTestRunner) { controller =>
          val result = controller.postNewTask(FakeRequest("POST", "ignore").withFormUrlEncodedBody("taskDescription" -> "1", "solutionTemplate" -> "2", "referenceSolution" -> "3", "suite" -> "4"))
          status(result) mustBe BAD_REQUEST
          contentAsString(result) must include("""class="error"></dd>""")
        }
      }
    }
    "post correct addTask" should {
      "persist and redirect" in {
        val scalaTestRunner = mock[ScalaTestRunnerContract]
        scalaTestRunner.execSuite _ expects("3", "4") returns "passed"
        val dao = mock[Dao]
        dao.addTask _ expects Task(scalaClass, "1", "2", "3", "4") returns Future.successful(())
        withNewTaskController(dao, scalaTestRunner) { controller =>
          val result = controller.postNewTask(FakeRequest("POST", "ignore").withFormUrlEncodedBody("taskDescription" -> "1", "solutionTemplate" -> "2", "referenceSolution" -> "3", "suite" -> "4"))
          status(result) mustBe SEE_OTHER
          redirectLocation(result) mustBe Some("/")
        }
      }
    }
  }
}

object ControllerTest {
  def withNewTaskController[T](dao: Dao, scalaTestRunner: ScalaTestRunnerContract)(block: NewTask => T): T = {
    block(new NewTask(dao, scalaTestRunner, new MockMessageApi))
  }
}

package controllers

import controllers.ControllerTest._
import dal.Dao
import models.TaskType.{TaskType, scalaClass}
import models.{User, TaskType, Task}
import org.scalamock.scalatest.MockFactory
import org.scalatestplus.play.PlaySpec
import play.api.test.Helpers._
import play.api.test._
import service.{ScalaTestRunner, ScalaTestRunnerContract}

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
    "post FAILED addTask" should {
      "fall in ScalaTestRunner" in {
        val solution = "class SubArrayWithMaxSum {\n  def apply(a: Array[Int]): Array[Int] = {\n    var currentSum = 0\n    var maxSum = 0\n    var left, right = 0\n    var maxI = 0 //used when all negatives in the array\n\n    for (i <- a.indices) {\n      val incSum = currentSum + a(i)\n\n      if (incSum > 0) {\n        currentSum = incSum\n\n        if (currentSum > maxSum) {\n          maxSum = currentSum\n          right = i\n        }\n      } else {\n        left = i + 1\n        right = left\n        currentSum = 0\n        if (a(i) > a(maxI)) maxI = i\n      }\n    }\n\n    if (left == a.length) a.slice(maxI, maxI + 1)\n    else a.slice(left, right + 1)\n  }\n}"
        val suite = "import org.scalatest.{FlatSpec, Matchers}\n\nclass SubArrayWithMaxSumTest(solution: SubArrayWithMaxSumSolution) extends FlatSpec with Matchers {\n  behavior of \"SubArrayWithMaxSum\"\n\n  it should \"return max sum sub array within given array\" in {\n    solution.apply(Array(-2, 1, -3, 4, -1, 2, 1, -5, 4)) should be(Array(4, -1, 2, 1))\n    solution.apply(Array(-2, 1, -3, 4, -1, 2, 1, 5, 4)) should be(Array(4, -1, 2, 1, 5, 4))\n    solution.apply(Array(2, -1, 0, 0, 0, 0, 1)) should be(Array(2))\n  }\n\n  it should \"return the whole array when given array has only positive numbers\" in {\n    solution.apply(Array(2, 1, 3, 4, 1, 2, 1, 5, 4)) should be(Array(2, 1, 3, 4, 1, 2, 1, 5, 4))\n  }\n\n  it should \"return max sum sub array when given array contains only negative numbers\" in {\n    solution.apply(Array(-2, -1, -3, -4, -1, -2, -1, -5, -4)) should be(Array(-1))\n    solution.apply(Array(-2, -3, -3, -4, -6, -2, -6, -5, -1)) should be(Array(-2))\n  }\n}\n\ntrait SubArrayWithMaxSumSolution {\n  def apply(a: Array[Int]): Array[Int]\n}"
        val dao = new Dao {
          def addTask(task: Task) = Future.successful(())
          def create(user: User) = ???
          def getTasks(`type`: TaskType, limit: Port, yearAgo: Port) = ???
        }
        withNewTaskController(dao, ScalaTestRunner) { controller =>
          val result = controller.postNewTask(FakeRequest("POST", "ignore").withFormUrlEncodedBody("taskDescription" -> "1", "solutionTemplate" -> "2", "referenceSolution" -> solution, "suite" -> suite))
          status(result) mustBe BAD_REQUEST
          contentAsString(result) must include("""class="error"></dd>""")
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

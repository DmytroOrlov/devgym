package controllers

import dal.Dao
import models.NewTask
import models.TaskType._
import monifu.concurrent.Implicits.globalScheduler
import monifu.concurrent.Scheduler
import org.scalamock.scalatest.MockFactory
import org.scalatestplus.play.PlaySpec
import play.api.test.Helpers._
import play.api.test._
import service._

import scala.concurrent.Future

class ControllerTest extends PlaySpec with MockFactory {

  "AddTask controller" when {
    "post fail with scalaTestRunner when addTask" should {
      "result BadRequest with error" in {
        //given
        val scalaTestRunner = mock[DynamicSuiteExecutor]
        (scalaTestRunner.apply(_: String, _: String)(_: String => Unit)(_: Scheduler)) expects("4", "5", *, *)
        //when
        withAddTaskController(scalaTestRunner)({ controller =>
          val result = controller.postNewTask(FakeRequest("POST", "ignore")
            .withFormUrlEncodedBody("taskName" -> "1", "taskDescription" -> "2", "solutionTemplate" -> "3",
              "referenceSolution" -> "4", "suite" -> "5"))
          //then
          status(result) mustBe BAD_REQUEST
          contentAsString(result) must include("id='errorReport'>")
        })
      }
    }
    "post fail with dao when addTask" should {
      "result BadRequest with error" in {
        //given
        val scalaTestRunner = mock[DynamicSuiteExecutor]
        (scalaTestRunner.apply(_: String, _: String)(_: String => Unit)(_: Scheduler)) expects("4", "5", *, *)
        val dao = mock[Dao]
        dao.addTask _ expects NewTask(scalaClass, "1", "2", "3", "4", "5") returns Future.failed(new RuntimeException)
        //when
        withAddTaskController(scalaTestRunner, dao)({ controller =>
          val result = controller.postNewTask(FakeRequest("POST", "ignore")
            .withFormUrlEncodedBody("taskName" -> "1", "taskDescription" -> "2", "solutionTemplate" -> "3",
              "referenceSolution" -> "4", "suite" -> "5"))
          //then
          status(result) mustBe INTERNAL_SERVER_ERROR
          contentAsString(result) must include( """class="error"></dd>""")
        })
      }
    }
    "post correct addTask" should {
      "persist and redirect" in {
        //given
        val scalaTestRunner = mock[DynamicSuiteExecutor]
        (scalaTestRunner.apply(_: String, _: String)(_: String => Unit)(_: Scheduler)) expects("4", "5", *, *)
        val dao = mock[Dao]
        dao.addTask _ expects NewTask(scalaClass, "1", "2", "3", "4", "5") returns Future.successful(())
        //when
        withAddTaskController(scalaTestRunner, dao)({ controller =>
          val result = controller.postNewTask(FakeRequest("POST", "ignore")
            .withFormUrlEncodedBody("taskName" -> "1", "taskDescription" -> "2", "solutionTemplate" -> "3",
              "referenceSolution" -> "4", "suite" -> "5"))
          //then
          status(result) mustBe SEE_OTHER
          redirectLocation(result) mustBe Some("/addTask")
        })
      }
    }
    "post FAILED addTask" should {
      "fall in ScalaTestRunner" in {
        //given
        val solution = "class SubArrayWithMaxSum {\n  def apply(a: Array[Int]): Array[Int] = {\n    var currentSum = 0\n    var maxSum = 0\n    var left, right = 0\n    var maxI = 0 //used when all negatives in the array\n\n    for (i <- a.indices) {\n      val incSum = currentSum + a(i)\n\n      if (incSum > 0) {\n        currentSum = incSum\n\n        if (currentSum > maxSum) {\n          maxSum = currentSum\n          right = i\n        }\n      } else {\n        left = i + 1\n        right = left\n        currentSum = 0\n        if (a(i) > a(maxI)) maxI = i\n      }\n    }\n\n    if (left == a.length) a.slice(maxI, maxI + 1)\n    else a.slice(left, right + 1)\n  }\n}"
        val badSuite = "import org.scalatest.{FlatSpec, Matchers}\n\nclass SubArrayWithMaxSumTest(solution: SubArrayWithMaxSumSolution) extends FlatSpec with Matchers {\n  behavior of \"SubArrayWithMaxSum\"\n\n  it should \"return max sum sub array within given array\" in {\n    solution.apply(Array(-2, 1, -3, 4, -1, 2, 1, -5, 4)) should be(Array(4, -1, 2, 1))\n    solution.apply(Array(-2, 1, -3, 4, -1, 2, 1, 5, 4)) should be(Array(4, -1, 2, 1, 5, 4))\n    solution.apply(Array(2, -1, 0, 0, 0, 0, 1)) should be(Array(2))\n  }\n\n  it should \"return the whole array when given array has only positive numbers\" in {\n    solution.apply(Array(2, 1, 3, 4, 1, 2, 1, 5, 4)) should be(Array(2, 1, 3, 4, 1, 2, 1, 5, 4))\n  }\n\n  it should \"return max sum sub array when given array contains only negative numbers\" in {\n    solution.apply(Array(-2, -1, -3, -4, -1, -2, -1, -5, -4)) should be(Array(-1))\n    solution.apply(Array(-2, -3, -3, -4, -6, -2, -6, -5, -1)) should be(Array(-2))\n  }\n}\n\ntrait SubArrayWithMaxSumSolution {\n  def apply(a: Array[Int]): Array[Int]\n}"
        val dao = stub[Dao]
        //when
        withAddTaskController(new ScalaTestRunner, dao)({ controller =>
          val result = controller.postNewTask(FakeRequest("POST", "ignore")
            .withFormUrlEncodedBody("taskName" -> "0", "taskDescription" -> "1",
              "solutionTemplate" -> "2", "referenceSolution" -> solution, "suite" -> badSuite))
          //then
          status(result) mustBe BAD_REQUEST
          contentAsString(result) must include("id='errorReport'>")
          dao.addTask _ verify NewTask(scalaClass, "0", "1", "2", solution, badSuite) never()
        })
      }
    }
  }

  def withAddTaskController[T](suiteExecutor: DynamicSuiteExecutor, dao: Dao = stub[Dao])(block: (AddTask) => T): T = {
    block(new AddTask(suiteExecutor, dao, new MockMessageApi))
  }
}

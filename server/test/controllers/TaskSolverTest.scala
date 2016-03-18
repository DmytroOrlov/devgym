package controllers

import java.util.{Date, UUID}

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import dal.Dao
import models.Task
import models.TaskType._
import monifu.concurrent.Implicits.globalScheduler
import org.scalamock.scalatest.MockFactory
import org.scalatestplus.play.{OneAppPerSuite, PlaySpec}
import play.api.cache.CacheApi
import play.api.test.FakeRequest
import play.api.test.Helpers._
import service.{DynamicSuiteExecutor, RuntimeSuiteExecutor}

import scala.concurrent.Future
import scala.concurrent.duration.FiniteDuration
import scala.reflect.ClassTag

class TaskSolverTest extends PlaySpec with MockFactory with OneAppPerSuite {
  implicit val system = ActorSystem()
  implicit val mat = ActorMaterializer()

  "TaskSolver controller" when {
    "getting available task to solve" should {
      "return template and task description" in {
        //given
        val dao = mock[Dao]
        val description = "some description"
        val template = "some template"
        val year = new Date()
        val timeuuid = new UUID(1, 1)
        val replyTask = Task(year, scalaClass, timeuuid, "array", description, template, "ref", "test suite")
        val taskSolver = new TaskSolver(mock[TestExecutor], dao, new MockMessageApi, mock[CacheApi])
        //when
        dao.getTask _ expects(year, scalaClass, timeuuid) returns Future.successful(Some(replyTask))
        val result = taskSolver.getTask(year.getTime, scalaClass.toString, timeuuid)(FakeRequest(GET, "ignore"))
        //then
        status(result) mustBe OK
        contentAsString(result) must (include(description) and include(template))
      }
    }
    "getting unavailable task to solve" should {
      "return to index page" in {
        //given
        val dao = mock[Dao]
        val taskSolver = new TaskSolver(mock[TestExecutor], dao, new MockMessageApi, mock[CacheApi])
        //when
        (dao.getTask _).expects(*, *, *).returning(Future.successful(None))
        val result = taskSolver.getTask(1, scalaClass.toString, new UUID(1, 1))(FakeRequest(GET, "ignore"))
        //then
        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some("/")
      }
    }
    "getting task from unstable dao" should {
      "return to index page" in {
        //given
        val dao = mock[Dao]
        val taskSolver = new TaskSolver(mock[TestExecutor], dao, new MockMessageApi, mock[CacheApi])
        //when
        (dao.getTask _).expects(*, *, *).throwing(new RuntimeException)
        val result = taskSolver.getTask(1, scalaClass.toString, new UUID(1, 1))(FakeRequest(GET, "ignore"))
        //then
        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some("/")
      }
    }
    "getting task multiple times" should {
      "return data from cache previously calling db once" in {
        //given
        val dao = stub[Dao]
        val cache = mock[CacheApi]
        val taskSolver = new TaskSolver(mock[TestExecutor], dao, new MockMessageApi, cache)
        val year = new Date()
        val timeuuid = new UUID(1, 1)
        val task = Task(year, scalaClass, timeuuid, "name", "descr", "template", "reference", "suite")

        //when
        (cache.getOrElse(_: String, _: FiniteDuration)(_: Any)(_: ClassTag[Any])).expects(*, *, *, *).throwing(new RuntimeException).once()
        (dao.getTask _).when(*, *, *).returns(Future.successful(Some(task))).once()
        (cache.set(_: String, _: Any, _: FiniteDuration)).expects(*, *, *).once()
        val result = taskSolver.getTask(year.getTime, scalaClass.toString, timeuuid)(FakeRequest(GET, "ignore"))
        //then
        status(result) mustBe OK

        //when
        (cache.getOrElse(_: String, _: FiniteDuration)(_: Any)(_: ClassTag[Any])).expects(*, *, *, *).returning(task).repeat(5)
        0 until 5 foreach { i =>
          val result2 = taskSolver.getTask(year.getTime, scalaClass.toString, timeuuid)(FakeRequest(GET, "ignore"))
          //then
          status(result2) mustBe OK
        }
        dao.getTask _ verify(*, *, *) once()
      }
    }
    "getting task from unstable db" should {
      "not update cache unless db is stable again" in {
        //given
        val dao = mock[Dao]
        val year = new Date()
        val timeuuid = new UUID(1, 1)
        val cache = mock[CacheApi]
        val taskSolver = new TaskSolver(mock[TestExecutor], dao, new MockMessageApi, cache)
        val task = Task(year, scalaClass, timeuuid, "name", "descr", "template", "reference", "suite")

        //when
        (dao.getTask _).expects(*, *, *).returning(Future.failed(new RuntimeException("unstable db"))).once()
        (cache.getOrElse(_: String, _: FiniteDuration)(_: Any)(_: ClassTag[Any])).expects(*, *, *, *)
          .throwing(new RuntimeException("Cache is empty")).twice()
        val badResult = taskSolver.getTask(year.getTime, scalaClass.toString, timeuuid)(FakeRequest(GET, "ignore"))
        //then
        status(badResult) mustBe SEE_OTHER

        //given
        val anotherYear = new Date(1000000)
        //when
        (dao.getTask _).expects(*, *, *).returning(Future.successful(Some(task))).once()
        (cache.set(_: String, _: Any, _: FiniteDuration)).expects(*, *, *).once()
        val goodResult = taskSolver.getTask(anotherYear.getTime, scalaClass.toString, timeuuid)(FakeRequest(GET, "ignore"))
        //then
        status(goodResult) mustBe OK

        //when
        (cache.getOrElse(_: String, _: FiniteDuration)(_: Any)(_: ClassTag[Any])).expects(*, *, *, *).returning(task).once()
        val goodResult2 = taskSolver.getTask(anotherYear.getTime, scalaClass.toString, timeuuid)(FakeRequest(GET, "ignore"))
        //then
        status(goodResult2) mustBe OK
      }
    }
  }

  abstract class TestExecutor extends RuntimeSuiteExecutor with DynamicSuiteExecutor {}
}

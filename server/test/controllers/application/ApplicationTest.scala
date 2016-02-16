package controllers.application

import java.time.{LocalDate, ZoneOffset}
import java.util.{Date, UUID}

import org.scalatestplus.play._
import play.api.test.Helpers._
import play.api.test._

class ApplicationTest extends PlaySpec with OneAppPerSuite {

  "Application" when {
    "get root" should {
      "result with index" in {
        val Some(result) = route(FakeRequest(GET, "/"))
        status(result) mustBe OK
        contentAsString(result) must (include("/task") and include("/addTask") and include("/register")
          and include( """<li><a href="/task/"""))
      }
    }
    "get logout" should {
      "redirect" in {
        val Some(result) = route(FakeRequest(GET, "/logout"))
        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some("/")
      }
    }
  }
  "AddTask" when {
    "get addTask" should {
      "result with form" in {
        val Some(result) = route(FakeRequest(GET, "/addTask"))

        status(result) mustBe OK
        contentAsString(result) must (
          include("<form") and include("/addTask") and include("taskName")
            and include("taskDescription") and include("solutionTemplate") and include("referenceSolution")
            and include("suite") and include("suite") and include( """<input type="submit""""))
      }
    }
    "post form with missed fields to addTask" should {
      "result BadRequest with error1" in {
        val Some(result) = route(FakeRequest(POST, "/addTask").withFormUrlEncodedBody("taskName" -> "0", "taskDescription" -> "1", "solutionTemplate" -> "2", "referenceSolution" -> "3" /*, "suite" -> "4"*/))

        status(result) mustBe BAD_REQUEST
        contentAsString(result) must (
          include("<form") and include("/addTask") and include("taskDescription") and include("solutionTemplate")
            and include("referenceSolution") and include("suite") and include("suite") and include( """<input type="submit""""))
        contentAsString(result) must (include( """class="error"""") and include("This field is required"))
      }
      "result BadRequest with error2" in {
        val Some(result) = route(FakeRequest(POST, "/addTask").withFormUrlEncodedBody("taskName" -> "0", "taskDescription" -> "1", "solutionTemplate" -> "2", /*"referenceSolution" -> "3",*/ "suite" -> "4"))

        status(result) mustBe BAD_REQUEST
        contentAsString(result) must (include("<form") and include("/addTask") and include("taskDescription")
          and include("solutionTemplate") and include("referenceSolution") and include("suite") and include("suite")
          and include( """<input type="submit""""))
        contentAsString(result) must (include( """class="error"""") and include("This field is required"))
      }
      "result BadRequest with error3" in {
        val Some(result) = route(FakeRequest(POST, "/addTask").withFormUrlEncodedBody("taskName" -> "0", "taskDescription" -> "1", /*"solutionTemplate" -> "2",*/ "referenceSolution" -> "3", "suite" -> "4"))

        status(result) mustBe BAD_REQUEST
        contentAsString(result) must (include("<form") and include("/addTask") and include("taskDescription")
          and include("solutionTemplate") and include("referenceSolution") and include("suite") and include("suite")
          and include( """<input type="submit""""))
        contentAsString(result) must (include( """class="error"""") and include("This field is required"))
      }
      "result BadRequest with error4" in {
        val Some(result) = route(FakeRequest(POST, "/addTask").withFormUrlEncodedBody("taskName" -> "0", /*"taskDescription" -> "1",*/ "solutionTemplate" -> "2", "referenceSolution" -> "3" /*, "suite" -> "4"*/))

        status(result) mustBe BAD_REQUEST
        contentAsString(result) must (include("<form") and include("/addTask") and include("taskDescription")
          and include("solutionTemplate") and include("referenceSolution") and include("suite") and include("suite")
          and include( """<input type="submit""""))
        contentAsString(result) must (include( """class="error"""") and include("This field is required"))
      }
    }
    "post form with bad solution to addTask" should {
      "result BadRequest with error" in {
        val Some(result) = route(FakeRequest(POST, "/addTask").withFormUrlEncodedBody("taskName" -> "0", "taskDescription" -> "1", "solutionTemplate" -> "2", "referenceSolution" -> "3", "suite" -> "4"))

        status(result) mustBe BAD_REQUEST
        contentAsString(result) must (include("<form") and include("/addTask") and include("taskDescription")
          and include("solutionTemplate") and include("referenceSolution") and include("suite") and include("suite")
          and include( """<input type="submit""""))
        contentAsString(result) must (include( """class="error"""") and include("Can not add your task"))
      }
    }
  }

  "TaskSolver" when {
    "get the available task" should {
      "result with form" in {
        val year = LocalDate.of(2016, 1, 1)
        val instant = year.atStartOfDay().atZone(ZoneOffset.UTC).toInstant

        val Some(result) = route(FakeRequest(GET, s"/task/scalaClass/${Date.from(instant).getTime}/9894cd10-ce12-11e5-8ee9-091830ac5256"))

        status(result) mustBe OK
        contentAsString(result) must (
          include("<form") and include("solution") and include("taskType") and include("timeuuid") and include("year")
            and include( """<input type="button""""))
      }
    }
    "get the unavailable task" should {
      "result BadRequest with Error" in {
        val Some(result) = route(FakeRequest(GET, s"/task/scalaClass/${new Date().getTime}/${new UUID(1, 1)}"))

        status(result) mustBe SEE_OTHER
        flash(result).get("flashToUser").get must be("Task does not exist")
      }
    }
  }
}

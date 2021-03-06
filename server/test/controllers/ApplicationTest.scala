package controllers

import java.time.{LocalDate, ZoneOffset}
import java.util.Date

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import controllers.TestParams.fakeSession
import org.scalatest.{CompositeStatus, Status, Args, DoNotDiscover}
import org.scalatestplus.play._
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.test.Helpers._
import play.api.test._
import tag.RequireDB

@DoNotDiscover
class ApplicationTest extends PlaySpec with ConfiguredApp {
  implicit val system = ActorSystem()
  implicit val mat = ActorMaterializer()

  "DevGymApp" when {
    "get logout" should {
      "redirect" in {
        val Some(result) = route(app, FakeRequest(GET, "/logout"))
        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some("/")
      }
    }
  }
  "AddTask" when {
    "get addTask" should {
      "result with non-logged user message" in {
        val Some(result) = route(app, FakeRequest(GET, "/addTask"))
        status(result) mustBe OK
        contentAsString(result) must include("Please login or register")
      }
    }
    "get addTask" should {
      "result with form" in {
        val Some(result) = route(app, FakeRequest(GET, "/addTask").withSession(fakeSession))

        status(result) mustBe OK
        contentAsString(result) must (
          include("<form") and include("/addTask") and include("taskName")
            and include("taskDescription") and include("solutionTemplate") and include("referenceSolution")
            and include("suite") and include("suite") and include( """<input type="submit""""))
      }
    }
    "post form with missed fields to addTask" should {
      "result BadRequest with error1" in {
        val Some(result) = route(app, FakeRequest(POST, "/addTask")
          .withFormUrlEncodedBody("taskName" -> "0", "taskDescription" -> "1", "solutionTemplate" -> "2", "referenceSolution" -> "3")
          .withSession(fakeSession))

        status(result) mustBe BAD_REQUEST
        contentAsString(result) must (
          include("<form") and include("/addTask") and include("taskDescription") and include("solutionTemplate")
            and include("referenceSolution") and include("suite") and include("suite") and include( """<input type="submit""""))
        contentAsString(result) must (include( """class="error"""") and include("This field is required"))
      }
      "result BadRequest with error2" in {
        val Some(result) = route(app, FakeRequest(POST, "/addTask")
          .withFormUrlEncodedBody("taskName" -> "0", "taskDescription" -> "1", "solutionTemplate" -> "2", "suite" -> "4")
          .withSession(fakeSession))

        status(result) mustBe BAD_REQUEST
        contentAsString(result) must (include("<form") and include("/addTask") and include("taskDescription")
          and include("solutionTemplate") and include("referenceSolution") and include("suite") and include("suite")
          and include( """<input type="submit""""))
        contentAsString(result) must (include( """class="error"""") and include("This field is required"))
      }
      "result BadRequest with error3" in {
        val Some(result) = route(app, FakeRequest(POST, "/addTask")
          .withFormUrlEncodedBody("taskName" -> "0", "taskDescription" -> "1", "referenceSolution" -> "3", "suite" -> "4")
          .withSession(fakeSession))

        status(result) mustBe BAD_REQUEST
        contentAsString(result) must (include("<form") and include("/addTask") and include("taskDescription")
          and include("solutionTemplate") and include("referenceSolution") and include("suite") and include("suite")
          and include( """<input type="submit""""))
        contentAsString(result) must (include( """class="error"""") and include("This field is required"))
      }
      "result BadRequest with error4" in {
        val Some(result) = route(app, FakeRequest(POST, "/addTask")
          .withFormUrlEncodedBody("taskName" -> "0", "solutionTemplate" -> "2", "referenceSolution" -> "3")
          .withSession(fakeSession))

        status(result) mustBe BAD_REQUEST
        contentAsString(result) must (include("<form") and include("/addTask") and include("taskDescription")
          and include("solutionTemplate") and include("referenceSolution") and include("suite") and include("suite")
          and include( """<input type="submit""""))
        contentAsString(result) must (include( """class="error"""") and include("This field is required"))
      }
    }
    "post form with bad solution to addTask" should {
      "result BadRequest with error" in {
        val Some(result) = route(app, FakeRequest(POST, "/addTask")
          .withFormUrlEncodedBody("taskName" -> "0", "taskDescription" -> "1", "solutionTemplate" -> "2", "referenceSolution" -> "3", "suite" -> "trait ST")
          .withSession(fakeSession))

        status(result) mustBe BAD_REQUEST
        contentAsString(result) must (include("<form") and include("/addTask") and include("taskDescription")
          and include("solutionTemplate") and include("referenceSolution") and include("suite") and include("suite")
          and include( """<input type="submit""""))
        contentAsString(result) must (include("id='errorReport'") and include("There is no class definition in solution code"))
      }
    }
  }

  "GitHubUser" when {
    "get login" should {
      "redirect to github.com" in {
        val Some(result) = route(app, FakeRequest(GET, "/githublogin"))
        status(result) mustBe SEE_OTHER
        redirectLocation(result).getOrElse("") must include("github.com")
      }
    }
  }
}

class ApplicationRequireDBTest extends PlaySpec with OneAppPerSuite {
  override def run(testName: Option[String], args: Args): Status = {
    if(expectedTestCount(args.filter) > 0) super.run(testName, args) else new CompositeStatus(Set.empty)
  }
  implicit override lazy val app = new GuiceApplicationBuilder()
    .configure(Map("ehcacheplugin" -> "disabled"))
    .build()
  implicit val system = ActorSystem()
  implicit val mat = ActorMaterializer()

  "DevGymApp" when {
    "get root" should {
      "result with index" taggedAs RequireDB in {
        val Some(result) = route(app, FakeRequest(GET, "/"))
        status(result) mustBe OK
        contentAsString(result) must (include("/task") and include("/addTask") and include("/register")
          and include( """<a href="/task/"""))
      }
    }
  }
  "TaskSolver" when {
    "get the available task" should {
      "result with form" taggedAs RequireDB in {
        val year = LocalDate.of(2017, 1, 1) // this test will fail in 2018, we need better test design here
        val instant = year.atStartOfDay().atZone(ZoneOffset.UTC).toInstant

        val Some(result) = route(app,
          FakeRequest(GET, s"/task/scalaLang/${Date.from(instant).getTime}/9894cd10-ce12-11e5-8ee9-091830ac5256"))

        status(result) mustBe OK
        contentAsString(result) must (
          include("<form") and include("solution") and include("lang") and include("timeuuid") and include("year")
            and include( """<input type="button""""))
      }
    }
    "get the unavailable task" should {
      "redirect to index with Error" taggedAs RequireDB in {
        val Some(result) = route(app, FakeRequest(GET, s"/task/scalaLang/${new Date().getTime}/00000000-0000-1000-0000-000000000000"))

        status(result) mustBe SEE_OTHER
        flash(result).get("flashToUser").get must be("Task does not exist")
      }
    }
  }
}

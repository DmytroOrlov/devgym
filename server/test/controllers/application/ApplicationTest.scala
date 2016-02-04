package controllers.application

import org.scalatestplus.play._
import play.api.test.Helpers._
import play.api.test._

class ApplicationTest extends PlaySpec with OneAppPerSuite {

  "Application" when {
    "get root" should {
      "result with index" in {
        val Some(result) = route(FakeRequest(GET, "/"))
        status(result) mustBe OK
        contentAsString(result) must (include("/task") and include("/addTask") and include("/register"))
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
  "NewTask" when {
    "get addTask" should {
      "result with OK" in {
        val Some(result) = route(FakeRequest(GET, "/addTask"))
        status(result) mustBe OK
        contentAsString(result) must (include("<form") and include("/addTask"))
      }
    }
    "post no form to addTask" should {
      "result BadRequest with form" in {
        val Some(result) = route(FakeRequest(POST, "/addTask"))
        status(result) mustBe BAD_REQUEST
        contentAsString(result) must (include("<form") and include("This field is required"))
      }
    }
  }
}

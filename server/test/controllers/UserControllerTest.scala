package controllers

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import controllers.TestParams.fakeSession
import dal.UserDao
import models.User
import monix.execution.Scheduler.Implicits.global
import org.scalamock.scalatest.MockFactory
import org.scalatest.DoNotDiscover
import org.scalatestplus.play.{ConfiguredApp, PlaySpec}
import play.api.test.FakeRequest
import play.api.test.Helpers._

import scala.concurrent.Future

@DoNotDiscover class UserControllerTest extends PlaySpec with MockFactory with ConfiguredApp {
  implicit val system = ActorSystem()
  implicit val mat = ActorMaterializer()

  "UserController" when {
    "getting login page" should {
      "return login form for non-logged user" in {
        //when
        val result = controller.getLogin(FakeRequest("GET", "/login"))
        //then
        status(result) mustBe OK
        contentAsString(result) must (include("name") and include("password"))
      }
    }
    "getting login page" should {
      "redirect to index for already logged in user" in {
        //when
        val result = controller.getLogin(FakeRequest("GET", "/login").withSession(fakeSession))
        //then
        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some("/")
      }
    }
    "login form" should {
      "authenticate registered user" in {
        //given
        val dao = mock[UserDao]
        val userController = new UserController(dao, new MockMessageApi)
        val password = "testpassword"
        val userName = "testname"
        val passwordWithSalt = UserController.toHashSalt(password, "12345")
        dao.find _ expects userName returns Future.successful(Some(User(userName, passwordWithSalt)))
        //when
        val result = userController.postLogin(FakeRequest("POST", "ignore")
          .withFormUrlEncodedBody("name" -> userName, "password" -> password))
        //then
        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some("/")
      }
    }
    "getting registration page" should {
      "return registration form" in {
        //when
        val result = controller.getRegister(FakeRequest("GET", "ignore"))
        //then
        status(result) mustBe OK
        contentAsString(result) must (include("name") and include("password") and include("verify"))
      }
    }
    "registration form" should {
      "register new user" in {
        //given
        val dao = mock[UserDao]
        val userController = new UserController(dao, new MockMessageApi)
        val userName = "testname"
        val password = "testpassword"
        dao.create _ expects * returns Future.successful(true)
        //when
        val result = userController.postRegister(FakeRequest("POST", "ignore")
          .withFormUrlEncodedBody("name" -> userName, "password" -> password, "verify" -> password))
        //then
        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some("/")
      }
    }
    "registration form" should {
      "not register new user with not matched passwords" in {
        //when
        val result = controller.postRegister(FakeRequest("POST", "ignore")
          .withFormUrlEncodedBody("name" -> "name", "password" -> "pass", "verify" -> "no match"))
        //then
        status(result) mustBe BAD_REQUEST
        redirectLocation(result) mustBe None
      }
    }
  }

  def controller = new UserController(mock[UserDao], new MockMessageApi)
}

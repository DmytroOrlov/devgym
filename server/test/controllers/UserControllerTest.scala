package controllers

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import dal.Dao
import models.User
import monifu.concurrent.Implicits.globalScheduler
import org.scalamock.scalatest.MockFactory
import org.scalatestplus.play.{OneAppPerSuite, PlaySpec}
import play.api.test.FakeRequest
import play.api.test.Helpers._

import scala.concurrent.Future

class UserControllerTest extends PlaySpec with MockFactory with OneAppPerSuite {
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
        val result = controller.getLogin(FakeRequest("GET", "/login").withSession("username" -> "user1"))
        //then
        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some("/")
      }
    }
    "login form" should {
      "authenticate registered user" in {
        //given
        val dao = mock[Dao]
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
        val dao = mock[Dao]
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
  }

  def controller = new UserController(mock[Dao], new MockMessageApi)
}

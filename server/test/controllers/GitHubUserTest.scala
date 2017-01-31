package controllers

import java.nio.file.Path

import org.scalamock.scalatest.MockFactory
import org.scalatest.DoNotDiscover
import org.scalatestplus.play.{ConfiguredApp, PlaySpec}
import play.api.libs.json.{JsObject, JsString}
import play.api.libs.ws.{WSClient, WSResponse}
import play.api.mvc.Result
import play.api.test.FakeRequest
import play.api.test.Helpers._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

@DoNotDiscover class GitHubUserTest extends PlaySpec with ConfiguredApp with MockFactory {
  private implicit lazy val s = app.actorSystem
  private implicit lazy val m = app.materializer
  private implicit lazy val ws = mock[WSClient]
  val secret = "secret"
  val accessToken = Map("access_token" -> JsString("toooken"))

  "GitHubUser" when {
    "github calls callback" should {
      "redirect to index having GitHub data in session" in {
        //given
        val gitHubUser = new GitHubUser(MockMessageApi, secret) {
          override def getToken(code: String)(implicit ws: WSClient) = {
            val response = mock[WSResponse]
            response.json _ expects() returns JsObject(accessToken)
            Future.successful(response)
          }

          override def query(token: String, path: Path)(implicit ws: WSClient) = {
            val response = mock[WSResponse]
            response.json _ expects() returns JsObject(Map(
              "login" -> JsString("alex"),
              "name" -> JsString("Alexey"),
              "avatar_url" -> JsString("avatar/url")
            ))
            Future.successful(response)
          }
        }

        //when
        val result = gitHubCallback(gitHubUser)

        //then
        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some("/")
        session(result).data must contain theSameElementsAs
          Map(loginName -> "alex", userName -> "Alexey", avatarUrl -> "avatar/url")
      }

      "fails when secret is not valid" in {
        //given
        val gitHubUser = new GitHubUser(MockMessageApi, "invalid secret")

        //when
        val result = gitHubCallback(gitHubUser)

        //then
        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some("/")
        flash(result).get(flashToUser) must be(Some(GitHubUser.cannotLoginViaGitHub))
      }

      "fails when token is not available" in {
        //given
        val gitHubUser = new GitHubUser(MockMessageApi, secret) {
          override def getToken(code: String)(implicit ws: WSClient) =
            Future.failed(new RuntimeException("test exception"))
        }

        //when
        val result = gitHubCallback(gitHubUser)

        //then
        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some("/")
        flash(result).get(flashToUser) must be(Some(GitHubUser.cannotLoginViaGitHub))
      }

      "fails when user is not available" in {
        //given
        val gitHubUser = new GitHubUser(MockMessageApi, secret) {
          override def getToken(code: String)(implicit ws: WSClient) = {
            val response = mock[WSResponse]
            response.json _ expects() returns JsObject(accessToken)
            Future.successful(response)
          }

          override def query(token: String, path: Path)(implicit ws: WSClient): Future[WSResponse] =
            Future.failed(new RuntimeException("test exception"))
        }

        //when
        val result = gitHubCallback(gitHubUser)

        //then
        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some("/")
        flash(result).get(flashToUser) must be(Some(GitHubUser.cannotLoginViaGitHub))
      }
    }
  }

  def gitHubCallback(gitHubUser: GitHubUser): Future[Result] = {
    gitHubUser.githubCallback("somecode", secret)(FakeRequest(GET, "ignore"))
  }
}

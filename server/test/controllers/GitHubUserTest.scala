package controllers

import akka.http.scaladsl.marshalling.Marshal
import akka.http.scaladsl.model.HttpResponse
import akka.http.scaladsl.model.Uri.{Path, Query}
import controllers.Response.AccessToken
import org.scalatest.DoNotDiscover
import org.scalatestplus.play.{ConfiguredApp, PlaySpec}
import play.api.mvc.Result
import play.api.test.FakeRequest
import play.api.test.Helpers._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

@DoNotDiscover class GitHubUserTest extends PlaySpec with ConfiguredApp {

  "GitHubUser" when {
    "github calls callback" should {
      "redirect to index having GitHub data in session" in {
        //given
        val gitHubUser = new GitHubUser(new MockMessageApi) {
          override def getToken(code: String) = Marshal(AccessToken("toooooken")).to[HttpResponse]

          override def query(token: String, path: Path, query: Query) =
            Marshal(GUser("alex", Some("Alexey"), "avatar/url")).to[HttpResponse]
        }

        //when
        val result = gitHubCallback(gitHubUser)

        //then
        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some("/")
        session(result).data must contain theSameElementsAs Map(loginName -> "alex", userName -> "Alexey", avatarUrl -> "avatar/url")
      }

      "fails when token is not available" in {
        //given
        val gitHubUser = new GitHubUser(new MockMessageApi) {
          override def getToken(code: String) = Future.failed(new RuntimeException)
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
        val gitHubUser = new GitHubUser(new MockMessageApi) {
          override def getToken(code: String) = Marshal(AccessToken("toooooken")).to[HttpResponse]

          override def query(token: String, path: Path, query: Query) = Future.failed(new RuntimeException)
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
    gitHubUser.githubCallback("somecode")(FakeRequest(GET, "ignore"))
  }
}

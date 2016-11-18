package controllers

import akka.http.scaladsl.marshalling.Marshal
import akka.http.scaladsl.model.HttpResponse
import akka.http.scaladsl.model.Uri.{Path, Query}
import controllers.Response.AccessToken
import org.scalatest.DoNotDiscover
import org.scalatestplus.play.{ConfiguredApp, PlaySpec}
import play.api.test.FakeRequest
import play.api.test.Helpers._

import scala.concurrent.ExecutionContext.Implicits.global

@DoNotDiscover class GitHubUserTest extends PlaySpec with ConfiguredApp {

  "GitHubUser" when {
    "github calls callback" should {
      "redirect to index having GitHub data in session" in {
        //given
        val gitHubUser = new GitHubUser() {
          override def getToken(code: String) = Marshal(AccessToken("toooooken")).to[HttpResponse]

          override def query(token: String, path: Path, query: Query) =
            Marshal(GUser("alex", Some("Alexey"), "avatar/url")).to[HttpResponse]
        }

        //when
        val result = gitHubUser.githubCallback("somecode")(FakeRequest(GET, "ignore"))

        //then
        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some("/")
        session(result).data must be eq Map(loginName -> "alex", userName -> "Alexey", avatarUrl -> "avatar/url")
      }
    }
  }

}

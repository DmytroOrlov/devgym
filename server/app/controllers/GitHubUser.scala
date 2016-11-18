package controllers

import akka.http.scaladsl.model.Uri
import akka.http.scaladsl.model.Uri.{Path, Query}
import akka.http.scaladsl.unmarshalling.Unmarshal
import com.google.inject.Inject
import controllers.Response.AccessToken
import play.api.mvc.{Action, Controller}

import scala.concurrent.ExecutionContext

case class GUser(login: String, name: Option[String], avatar_url: String)

object Response {

  case class AccessToken(access_token: String)

}

class GitHubUser @Inject()(implicit ec: ExecutionContext)
  extends Controller with Json4sSupport with GitHubServer {

  def getLogin = Action { implicit request =>
    Redirect(Uri("https://github.com/login/oauth/authorize")
      .withQuery(Query(
        "client_id" -> clientId,
        "state" -> "some_state"
      )).toString()
    )
  }

  def githubCallback(code: String) = Action.async { implicit request =>

    def access(code: String) = {
      getToken(code).flatMap(response => Unmarshal(response).to[AccessToken].map(_.access_token))
    }

    def fetchUser(token: String) = query(token, Path.Empty / "user").flatMap(response => Unmarshal(response).to[GUser])

    for {
      token <- access(code)
      userInfo <- fetchUser(token)
    } yield {
      Redirect(routes.Application.index).withSession(
        loginName -> userInfo.login, userName -> userInfo.name.getOrElse(""), avatarUrl -> userInfo.avatar_url)
    }
  }
}



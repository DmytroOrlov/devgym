package controllers

import javax.inject.Inject

import akka.http.scaladsl.model.Uri
import akka.http.scaladsl.model.Uri.{Path, Query}
import akka.http.scaladsl.unmarshalling.Unmarshal
import controllers.GitHubUser._
import controllers.Response.AccessToken
import play.api.i18n.MessagesApi
import play.api.mvc.{Action, Controller}

import scala.concurrent.ExecutionContext
import scala.util.control.NonFatal

case class GUser(login: String, name: Option[String], avatar_url: String)

object Response {

  case class AccessToken(access_token: String)

}

class GitHubUser @Inject()(val messagesApi: MessagesApi)(implicit ec: ExecutionContext)
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

    (for {
      token <- access(code)
      userInfo <- fetchUser(token)
    } yield {
      Redirect(routes.Application.index).withSession(
        loginName -> userInfo.login, userName -> userInfo.name.getOrElse(""), avatarUrl -> userInfo.avatar_url)
    }) recover {
      case NonFatal(e) => Redirect(routes.Application.index).flashing(flashToUser -> messagesApi(cannotLoginViaGitHub))
    }
  }
}

object GitHubUser {
  val cannotLoginViaGitHub = "cannotLoginViaGitHub"
}



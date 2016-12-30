package controllers

import java.nio.file.Paths
import javax.inject.{Inject, Named}

import controllers.GitHubUser._
import play.api.Logger
import play.api.i18n.MessagesApi
import play.api.libs.json.Json
import play.api.libs.ws.WSClient
import play.api.mvc.{Action, Controller}

import scala.concurrent.{ExecutionContext, Future}
import scala.util.control.NonFatal

case class GUser(login: String, name: Option[String], avatar_url: String)
case class AccessToken(access_token: String)

class GitHubUser @Inject()(val messagesApi: MessagesApi, @Named("Secret") secret: String)
                          (implicit ec: ExecutionContext, ws: WSClient)
  extends Controller with GitHubServer {

  private implicit val guserReads = Json.reads[GUser]
  private implicit val tokenReads = Json.reads[AccessToken]

  def getLogin = Action {
    Redirect(ws.url("https://github.com/login/oauth/authorize")
      .withQueryString(
        "client_id" -> clientId,
        "state" -> secret
      )
      .uri
      .toString)
  }

  def githubCallback(code: String, state: String) = Action.async {

    def checkSecret(state: String) = {
      if (state == secret) Future.successful("Ok")
      else Future.failed(new RuntimeException("GitHub 'state' value is not recognized"))
    }

    def access(code: String): Future[String] = {
      getToken(code).map(response => response.json.validate[AccessToken].asOpt.get.access_token)
    }

    def fetchUser(token: String) =
      query(token, Paths.get("user"))
        .map(response => response.json.validate[GUser].asOpt.get)

    (for {
      _ <- checkSecret(state)
      token <- access(code)
      userInfo <- fetchUser(token)
    } yield {
      Redirect(routes.Application.index).withSession(
        loginName -> userInfo.login, userName -> userInfo.name.getOrElse(""), avatarUrl -> userInfo.avatar_url)
    }) recover {
      case NonFatal(e) =>
        Logger.error("GitHub OAuth callback failed.", e)
        Redirect(routes.Application.index).flashing(flashToUser -> messagesApi(cannotLoginViaGitHub))
    }
  }
}

object GitHubUser {
  val cannotLoginViaGitHub = "cannotLoginViaGitHub"
}



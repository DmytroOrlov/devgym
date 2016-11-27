package controllers

import javax.inject.{Inject, Named}

import akka.actor.ActorSystem
import akka.http.scaladsl.model.Uri
import akka.http.scaladsl.model.Uri.{Path, Query}
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.stream.Materializer
import controllers.GitHubUser._
import controllers.Response.AccessToken
import play.api.Logger
import play.api.i18n.MessagesApi
import play.api.mvc.{Action, Controller}

import scala.concurrent.{ExecutionContext, Future}
import scala.util.control.NonFatal

case class GUser(login: String, name: Option[String], avatar_url: String)

object Response {
  case class AccessToken(access_token: String)
}

class GitHubUser @Inject()(val messagesApi: MessagesApi, @Named("Secret") secret: String)
                          (implicit ec: ExecutionContext, s: ActorSystem, m: Materializer)
  extends Controller with Json4sSupport with GitHubServer {

  def getLogin = Action {
    Redirect(Uri("https://github.com/login/oauth/authorize")
      .withQuery(Query(
        "client_id" -> clientId,
        "state" -> secret
      )).toString()
    )
  }

  def githubCallback(code: String, state: String) = Action.async {

    def checkSecret(state: String) = {
      if (state == secret) Future.successful("Ok")
      else Future.failed(new RuntimeException("GitHub 'state' value is not recognized"))
    }

    def access(code: String) = {
      getToken(code).flatMap(response => Unmarshal(response).to[AccessToken].map(_.access_token))
    }

    def fetchUser(token: String) = query(token, Path.Empty / "user").flatMap(response => Unmarshal(response).to[GUser])

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



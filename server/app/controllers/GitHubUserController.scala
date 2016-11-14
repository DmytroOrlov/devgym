package controllers

import akka.http.scaladsl.Http
import akka.http.scaladsl.model.Uri.{Path, Query}
import akka.http.scaladsl.model.headers.{Accept, Authorization, GenericHttpCredentials}
import akka.http.scaladsl.model.{HttpMethods, HttpRequest, MediaTypes, Uri}
import akka.http.scaladsl.unmarshalling.Unmarshal
import com.google.inject.Inject
import com.typesafe.config.ConfigFactory
import controllers.GitHubUserController._
import controllers.Response.AccessToken
import play.api.i18n.MessagesApi
import play.api.mvc.{Action, Controller}

import scala.concurrent.{ExecutionContext, Future}

case class GUser(login: String, name: Option[String], avatar_url: String)

object Response {
  case class AccessToken(access_token: String)
}

class GitHubUserController @Inject()(val messages: MessagesApi)(implicit ec: ExecutionContext)
  extends Controller with Json4sSupport {

  def getLogin = Action { implicit request =>
    Redirect(Uri("https://github.com/login/oauth/authorize")
      .withQuery(Query(
        "client_id" -> clientId,
        "state" -> "some_state"
      )).toString()
    )
  }

  def githubCallback(code: String) = Action.async { implicit request =>

    def access(code: String): Future[String] = {
      Http()
        .singleRequest(
          HttpRequest(
            method = HttpMethods.POST,
            uri = Uri("https://github.com/login/oauth/access_token").withQuery(
              Query(
                "client_id" -> clientId,
                "client_secret" -> clientSecret,
                "code" -> code,
                "redirect_uri" -> redirectUri
              )),
            headers = List(Accept(MediaTypes.`application/json`))
          ))
        .flatMap(response => Unmarshal(response).to[AccessToken].map(_.access_token))
    }

    def fetchGithub(token: String, path: Path, query: Query = Query.Empty) = {
      Http().singleRequest(
        HttpRequest(
          uri = Uri(s"https://api.github.com").withPath(path).withQuery(query),
          headers = List(Authorization(GenericHttpCredentials("token", token)))
        ))
    }

    def fetchUser(token: String) =
      fetchGithub(token, Path.Empty / "user").flatMap(response => Unmarshal(response).to[GUser])

    for {
      token <- access(code)
      userInfo <- fetchUser(token) //TODO: save the entire userInfo model either as separate props in cookie or on server
    } yield {
      Redirect(routes.Application.index).withSession(user -> userInfo.login)
    }
  }
}

object GitHubUserController {
  val config = ConfigFactory.load().getConfig("devgym.github")
  val clientId = config.getString("client-id")
  val clientSecret = config.getString("client-secret")
  val redirectUri = config.getString("uri") + "/githubback"
}



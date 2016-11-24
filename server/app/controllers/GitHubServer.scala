package controllers

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.Uri.{Path, Query}
import akka.http.scaladsl.model._
import akka.http.scaladsl.model.headers.{Accept, Authorization, GenericHttpCredentials}
import akka.stream.Materializer
import com.typesafe.config.ConfigFactory

import scala.concurrent.Future


trait GitHubServer {
  val config = ConfigFactory.load().getConfig("devgym.github")
  val clientId = config.getString("client-id")
  val clientSecret = config.getString("client-secret")
  val redirectUri = config.getString("uri") + "/githubback"

  def getToken(code: String)(implicit s: ActorSystem, m: Materializer): Future[HttpResponse] = {
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
  }

  def query(token: String, path: Path, query: Query = Query.Empty)(implicit s: ActorSystem, m: Materializer): Future[HttpResponse] = {
    Http().singleRequest(
      HttpRequest(
        uri = Uri(s"https://api.github.com").withPath(path).withQuery(query),
        headers = List(Authorization(GenericHttpCredentials("token", token)))
      ))
  }
}

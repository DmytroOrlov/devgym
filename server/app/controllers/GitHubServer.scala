package controllers

import java.nio.file.Path

import com.typesafe.config.ConfigFactory
import play.api.libs.ws.{WSClient, WSResponse}

import scala.concurrent.Future


trait GitHubServer {
  val config = ConfigFactory.load().getConfig("devgym.github")
  val clientId = config.getString("client-id")
  val clientSecret = config.getString("client-secret")
  val redirectUri = config.getString("uri")

  def getToken(code: String)(implicit ws: WSClient): Future[WSResponse] = {
    ws.url("https://github.com/login/oauth/access_token")
      .withQueryString(
        "client_id" -> clientId,
        "client_secret" -> clientSecret,
        "code" -> code,
        "redirect_uri" -> redirectUri
      )
      .withHeaders("Accept" -> "application/json")
      .withMethod("POST")
      .get()
  }

  def query(token: String, path: Path)(implicit ws: WSClient): Future[WSResponse] = {
    ws.url(s"https://api.github.com/$path")
      .withHeaders("Authorization" -> s"token $token")
      .get()
  }
}

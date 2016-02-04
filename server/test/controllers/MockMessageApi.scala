package controllers

import play.api.i18n.{Lang, Messages, MessagesApi}
import play.api.mvc
import play.api.mvc.Result
import play.mvc.Http.RequestHeader

class MockMessageApi extends MessagesApi {
  override def messages: Map[String, Map[String, String]] = ???

  override def preferred(candidates: Seq[Lang]): Messages = ???

  override def preferred(request: mvc.RequestHeader): Messages = Messages(Lang("en"), this)

  override def preferred(request: RequestHeader): Messages = ???

  override def langCookieHttpOnly: Boolean = ???

  override def clearLang(result: Result): Result = ???

  override def langCookieSecure: Boolean = ???

  override def langCookieName: String = ???

  override def setLang(result: Result, lang: Lang): Result = ???

  override def apply(key: String, args: Any*)(implicit lang: Lang): String = ""

  override def apply(keys: Seq[String], args: Any*)(implicit lang: Lang): String = ???

  override def isDefinedAt(key: String)(implicit lang: Lang): Boolean = ???

  override def translate(key: String, args: Seq[Any])(implicit lang: Lang): Option[String] = ???
}

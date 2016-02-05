package controllers

import play.api.libs.json._
import shared.Line

trait JSONFormats {
  private val defaultPointFormat = Json.format[Line]

  implicit val pointFormat = new Format[Line] {
    def reads(json: JsValue): JsResult[Line] =
      (json \ "event").validate[String].flatMap {
        case "line" =>
          defaultPointFormat.reads(json)
        case _ =>
          JsError(JsPath \ "event", s"Event is not `line`")
      }

    def writes(o: Line): JsValue =
      Json.obj("event" -> o.event) ++
        defaultPointFormat.writes(o).as[JsObject]
  }
}

package controllers

import play.api.libs.json._
import shared.model.{Event, Line, TestResult}

trait JSONFormats {
  private val lineFormat = Json.format[Line]
  private val testResultFormat = Json.format[TestResult]

  implicit val eventFormat = new Format[Event] {
    def reads(json: JsValue): JsResult[Event] =
    (json \ "name").validate[String].flatMap {
      case Line.name =>
        lineFormat.reads(json)
      case TestResult.name =>
        testResultFormat.reads(json)
      case e =>
        JsError(JsPath \ "name", s"Event '$e' is not supported")
    }

    def writes(event: Event): JsValue = {
      val jsObject = event match {
        case l: Line =>
          lineFormat.writes(l).as[JsObject]
        case tr: TestResult =>
          testResultFormat.writes(tr).as[JsObject]
      }

      Json.obj("name" -> event.name) ++ jsObject
    }
  }
}

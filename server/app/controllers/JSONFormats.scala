package controllers

import play.api.libs.json._
import shared.model._

trait JSONFormats {
  private val lineFormat = Json.format[Line]
  private val testResultFormat = Json.format[TestResult]
  private val compilingFormat = Json.format[Compiling]
  private val solutionTemplateFormat = Json.format[SolutionTemplate]

  implicit val eventFormat = new Format[Event] {
    def reads(json: JsValue): JsResult[Event] =
      (json \ "name").validate[String].flatMap {
        case Line.name =>
          lineFormat.reads(json)
        case TestResult.name =>
          testResultFormat.reads(json)
        case Compiling.name =>
          compilingFormat.reads(json)
        case SolutionTemplate.name =>
          solutionTemplateFormat.reads(json)
        case e =>
          JsError(JsPath \ "name", s"Event '$e' is not supported")
      }

    def writes(event: Event): JsValue = {
      val jsObject = event match {
        case l: Line =>
          lineFormat.writes(l).as[JsObject]
        case tr: TestResult =>
          testResultFormat.writes(tr).as[JsObject]
        case c: Compiling =>
          compilingFormat.writes(c).as[JsObject]
        case s: SolutionTemplate =>
          solutionTemplateFormat.writes(s).as[JsObject]
      }

      Json.obj("name" -> event.name) ++ jsObject
    }
  }
}

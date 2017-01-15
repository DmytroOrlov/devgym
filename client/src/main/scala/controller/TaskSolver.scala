package controller

import java.util.Date

import common.CodeEditor
import monix.execution.Cancelable
import monix.execution.Scheduler.Implicits.global
import monix.execution.cancelables.BooleanCancelable
import monix.reactive.observers.Subscriber
import monix.reactive.{Observable, OverflowStrategy}
import org.scalajs.dom
import org.scalajs.dom.XMLHttpRequest
import org.scalajs.dom.raw.ProgressEvent
import org.scalajs.jquery.jQuery
import shared.model._

import scala.scalajs.js
import scala.scalajs.js.Dynamic.{literal => obj}
import scala.scalajs.js.{JSApp, JSON}

object TaskSolver extends JSApp {
  private val loadingIcon = jQuery("#icon")
  val editor = new CodeEditor("solution")
  var prevTimestamp = 0L

  def main(): Unit = {
    initSubmitter("submit", to = "report")
  }

  def initSubmitter(buttonId: String, to: String) = {
    // submit by click
    val submitButton = jQuery(s"#$buttonId")
    submitButton.click(submit _)

    // submit by keyboard shortcut
    editor.bindShortcut("submitSolution", "Ctrl+Shift+S", "Ctrl+Shift+S", submitSolution)

    def submitSolution(editor: Any): Unit = {
      if (!isSubmitInProgress)
        submit()
    }

    def isSubmitInProgress = submitButton.prop("disabled").asInstanceOf[Boolean]

    def disableButton(flag: Boolean = true): Unit = submitButton.prop("disabled", flag)

    def submit() = {
      disableButton()
      loadingIcon.show()
      val testExecution = new TestExecution
      testExecution.subscribe(new TaskSolverReport(
        to,
        () => {
          disableButton(false)
          loadingIcon.hide()
        }))
    }
  }

  final class TestExecution extends Observable[Event] {
    def unsafeSubscribeFn(subscriber: Subscriber[Event]): Cancelable = {
      val currentTimestamp = System.currentTimeMillis()
      val prevTimestampCopy = prevTimestamp

      val source: Observable[Event] = Observable.create[String](OverflowStrategy.DropOld(100)) { downstream =>
        val xhr = new XMLHttpRequest()
        val cancelable = BooleanCancelable(() => xhr.abort())
        val url = "task-stream"
        xhr.open("POST", s"$protocol//$host/$url", async = true)
        xhr.responseType = "moz-chunked-text"
        xhr.onprogress = { (e: ProgressEvent) =>
          val resp = xhr.response.asInstanceOf[String]
          println(resp)
          downstream.onNext(resp)
        }
        xhr.onload = (e: dom.Event) => downstream.onComplete()
        xhr.onerror = (e: dom.Event) => downstream.onComplete()
        xhr.setRequestHeader("Content-type", "application/json")
        xhr.send(js.JSON.stringify(obj(
          "solution" -> editor.value,
          "year" -> jQuery("#year").`val`().asInstanceOf[String].toLong,
          "lang" -> jQuery("#lang").`val`().asInstanceOf[String],
          "timeuuid" -> jQuery("#timeuuid").`val`().asInstanceOf[String],
          "prevTimestamp" -> prevTimestampCopy,
          "currentTimestamp" -> currentTimestamp)))
        cancelable
      }.collect { case IsEvent(e) => e }
      val cancelable = (Observable(Line("Submitting...")) ++ source).subscribe(subscriber)
      prevTimestamp = currentTimestamp
      cancelable
    }

    def host = dom.window.location.host

    def protocol = dom.document.location.protocol
  }

  object IsEvent {
    def unapply(message: String): Option[Event] = {
      val json = JSON.parse(message)

      def getTimestamp = json.timestamp.asInstanceOf[Number].longValue()

      json.name.asInstanceOf[String] match {
        case Line.name => Some(Line(
          value = json.value.asInstanceOf[String],
          timestamp = getTimestamp
        ))
        case TestResult.name => Some(TestResult(
          status = json.status.asInstanceOf[String],
          errorMessage = json.errorMessage.asInstanceOf[String],
          timestamp = getTimestamp
        ))
        case Compiling.name => Some(Compiling(getTimestamp))
        case "error" =>
          val errorType = json.`type`.asInstanceOf[String]
          val message = json.message.asInstanceOf[String]
          throw new RuntimeException(s"Server-side error thrown (${new Date(getTimestamp)}) - $errorType: $message")
        case _ => None
      }
    }
  }

}

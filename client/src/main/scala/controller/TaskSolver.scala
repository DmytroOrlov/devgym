package controller

import java.util.Date

import client.SimpleWebSocketClient
import common.CodeEditor
import monifu.concurrent.Implicits.globalScheduler
import monifu.reactive.OverflowStrategy.DropOld
import monifu.reactive.{Observable, Subscriber}
import org.scalajs.dom
import org.scalajs.jquery.jQuery
import shared.model._

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
      val testExecution = new TestExecution()
      testExecution.subscribe(new TaskSolverReport(
        to,
        () => {
          disableButton(false)
          loadingIcon.hide()
        }))
    }
  }

  final class TestExecution extends Observable[Event] {
    def onSubscribe(subscriber: Subscriber[Event]) = {
      val host = dom.window.location.host
      val protocol = if (dom.document.location.protocol == "https:") "wss:" else "ws:"

      val currentTimestamp = System.currentTimeMillis()
      val prevTimestampCopy = prevTimestamp

      val source: Observable[Event] = new SimpleWebSocketClient(
        url = s"$protocol//$host/task-stream",
        DropOld(20),
        sendOnOpen = Some(obj(
          "solution" -> editor.value,
          "year" -> jQuery("#year").`val`().asInstanceOf[String].toLong,
          "lang" -> jQuery("#lang").`val`().asInstanceOf[String],
          "timeuuid" -> jQuery("#timeuuid").`val`().asInstanceOf[String],
          "prevTimestamp" -> prevTimestampCopy,
          "currentTimestamp" -> currentTimestamp
        ))
      ).collect { case IsEvent(e) => e }

      (Observable(Line("Submitting...")) ++ source).onSubscribe(subscriber)
      prevTimestamp = currentTimestamp
    }
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
          throw SimpleWebSocketClient.Exception(s"Server-side error thrown (${new Date(getTimestamp)}) - $errorType: $message")
        case _ => None
      }
    }
  }

}
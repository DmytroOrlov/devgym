package client

import java.util.Date

import common.CodeEditor
import monifu.concurrent.Implicits.globalScheduler
import monifu.reactive.Ack.Continue
import monifu.reactive.OverflowStrategy.DropOld
import monifu.reactive.{Ack, Observable, Observer, Subscriber}
import org.scalajs.dom
import org.scalajs.jquery.{JQuery, jQuery}
import shared.view.SuiteReportUtil._
import shared.model._

import scala.concurrent.Future
import scala.scalajs.js.Dynamic.{literal => obj}
import scala.scalajs.js.{JSApp, JSON}

object SubmitSolutionClient extends JSApp {
  val loadingIcon = jQuery("#icon")
  val editor = new CodeEditor("solution")

  def main(): Unit = initSubmitter("submit", to = "report")

  def initSubmitter(buttonId: String, to: String) = {
    val submitButton = jQuery(s"#$buttonId")
    submitButton.click(submit _)

    def disableButton(flag: Boolean = true): Unit = submitButton.prop("disabled", flag)

    def submit() = {
      loadingIcon.show()
      disableButton()
      val consumer = new TestExecution()
      consumer.subscribe(new Report(to, () => disableButton(false)))
    }
  }

  final class Report(reportId: String, onCompleteCall: () => Unit) extends Observer[Event] {
    val report = jQuery(s"#$reportId")
    report.empty()

    var compilationJustStarted = false

    override def onNext(elem: Event): Future[Ack] = {
      elem match {
        case l: Line => processLine(l)
        case tr: TestResult => processTestResult(tr)
        case _: Compiling => processCompiling()
        case _ => System.err.println(s"Event $elem is not supported")
      }
      Continue
    }

    private def processCompiling(): Unit = {
      report.append("Compiling...")
      compilationJustStarted = true
    }

    private def processTestResult(tr: TestResult) = {
      cleanReportAreaIfNeeded()

      val (result, cssClass) = tr.testStatus match {
        case TestStatus.Passed => ("Test Passed!", "testPassed")
        case TestStatus.Failed =>
          val error = Option(tr.errorMessage).filter(_.nonEmpty).map(_ + "<br/>").getOrElse("")
          (s"${error}Test Failed. Keep going!", "testFailed")
      }
      report.append(s"""<div class="$cssClass">$result</div>""")
    }

    private def processLine(l: Line): JQuery = {
      val line = removeToolboxText(replaceMarkers(l.value))
      cleanReportAreaIfNeeded()
      report.append(s"""<div>$line</div>""")
    }

    private def cleanReportAreaIfNeeded(): Unit = {
      if (compilationJustStarted) {
        compilationJustStarted = false
        report.html("<div class='result-output'>Result:</div>")
      }
    }

    def onComplete() = {
      loadingIcon.hide()
      onCompleteCall()
    }

    def onError(ex: Throwable) = {
      val m = s"${this.getClass.getName} $ex"
      System.err.println(m)
      report.append(m)
      onCompleteCall()
    }
  }

  final class TestExecution extends Observable[Event] {
    def onSubscribe(subscriber: Subscriber[Event]) = {
      val host = dom.window.location.host
      val protocol = if (dom.document.location.protocol == "https:") "wss:" else "ws:"

      val source: Observable[Event] = new SimpleWebSocketClient(
        url = s"$protocol//$host/task-stream",
        DropOld(20),
        sendOnOpen = Some(obj(
          "solution" -> editor.value,
          "year" -> jQuery("#year").`val`().asInstanceOf[String].toLong,
          "taskType" -> jQuery("#taskType").`val`().asInstanceOf[String],
          "timeuuid" -> jQuery("#timeuuid").`val`().asInstanceOf[String]
        ))
      ).collect { case IsEvent(e) => e }

      (Observable(Line("Submitting...")) ++ source).onSubscribe(subscriber)
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

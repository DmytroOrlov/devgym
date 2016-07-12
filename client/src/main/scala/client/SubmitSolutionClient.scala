package client

import java.util.Date

import monifu.concurrent.Implicits.globalScheduler
import monifu.reactive.Ack.Continue
import monifu.reactive.OverflowStrategy.DropOld
import monifu.reactive.{Ack, Observable, Observer, Subscriber}
import org.scalajs.dom
import org.scalajs.jquery.jQuery
import shared.view.SuiteReportUtil._
import shared.{Event, Line}

import scala.concurrent.Future
import scala.scalajs.js.Dynamic.{literal => obj}
import scala.scalajs.js.{JSApp, JSON}

object SubmitSolutionClient extends JSApp {
  val loadingIcon = jQuery("#icon")

  def main(): Unit = initSubmitter("submit", "solution", to = "report")

  def initSubmitter(buttonId: String, solutionId: String, to: String) = {
    val submitButton = jQuery(s"#$buttonId")
    submitButton.click(submit _)

    def disableButton(flag: Boolean): Unit = submitButton.prop("disabled", flag)

    def submit() = {
      loadingIcon.show()
      disableButton(true)
      val lines = new DataConsumer(solutionId).collect { case e: Line => e }
      lines.subscribe(new Report(to, () => disableButton(false)))
    }
  }

  final class Report(reportId: String, onCompleteCall: () => Unit) extends Observer[Line] {
    val report = {
      val r = jQuery(s"#$reportId")
      r.empty()
      r
    }

    override def onNext(elem: Line): Future[Ack] = {
      val line = removeToolboxText(replaceMarkers(elem.value))
      report.append( s"""<div>$line</div>""")
      Continue
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

  final class DataConsumer(solutionId: String) extends Observable[Event] {
    def onSubscribe(subscriber: Subscriber[Event]) = {
      val host = dom.window.location.host
      val protocol = if (dom.document.location.protocol == "https:") "wss:" else "ws:"

      val source = new SimpleWebSocketClient(
        url = s"$protocol//$host/task-stream",
        DropOld(20),
        sendOnOpen = Some(obj(
          "solution" -> jQuery(s"#$solutionId").`val`().asInstanceOf[String],
          "year" -> jQuery("#year").`val`().asInstanceOf[String].toLong,
          "taskType" -> jQuery("#taskType").`val`().asInstanceOf[String],
          "timeuuid" -> jQuery("#timeuuid").`val`().asInstanceOf[String]
        ))
      ).collect { case IsEvent(e) => e }

      (Observable.unit(Line("Submitting...")) ++ source)
        .onSubscribe(subscriber)
    }
  }

  object IsEvent {
    def unapply(message: String) = {
      val json = JSON.parse(message)

      json.event.asInstanceOf[String] match {
        case "line" => Some(Line(
          value = json.value.asInstanceOf[String],
          timestamp = json.timestamp.asInstanceOf[Number].longValue()
        ))
        case "error" =>
          val errorType = json.`type`.asInstanceOf[String]
          val message = json.message.asInstanceOf[String]
          val timestamp = json.timestamp.asInstanceOf[Number].longValue()
          throw new SimpleWebSocketClient.Exception(s"Server-side error thrown (${new Date(timestamp)}) - $errorType: $message")
        case _ => None
      }
    }
  }

}

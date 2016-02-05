package client

import monifu.concurrent.Implicits.globalScheduler
import monifu.reactive.Ack.Continue
import monifu.reactive.OverflowStrategy.DropOld
import monifu.reactive.{Ack, Observable, Observer, Subscriber}
import org.scalajs.dom
import org.scalajs.jquery.jQuery
import shared.{Event, Line}

import scala.concurrent.Future
import scala.scalajs.js.Dynamic.{literal => obj, _}
import scala.scalajs.js.JSApp

object SubmitSolutionClient extends JSApp {
  def main(): Unit = initSubmitter("submit", "solution", to = "report")

  def initSubmitter(buttonId: String, solutionId: String, to: String) = {
    def submit() = {
      val lines = new DataConsumer(solutionId).collect { case e: Line => e }
      lines.subscribe(new Report(to))
    }

    jQuery(s"#$buttonId").click(submit _)
  }

  final class Report(reportId: String) extends Observer[Line] {
    lazy val report = {
      val r = jQuery(s"#$reportId")
      r.empty()
      r
    }

    override def onNext(elem: Line): Future[Ack] = {
      report.append(s"${elem.value}\n")
      Continue
    }

    def onComplete() = ()

    def onError(ex: Throwable) = {
      val m = s"${this.getClass.getName} $ex"
      System.err.println(m)
      report.append(m)
    }
  }

  final class DataConsumer(solutionId: String) extends Observable[Event] {
    def onSubscribe(subscriber: Subscriber[Event]) = {
      val host = dom.window.location.host
      val protocol = if (dom.document.location.protocol == "https:") "wss:" else "ws:"

      val source = new SimpleWebSocketClient(
        url = s"$protocol//$host/task-stream",
        DropOld(20),
        sendOnOpen = Some(jQuery(s"#$solutionId").`val`().asInstanceOf[String])
      )
      source.collect { case IsEvent(e) => e }
        .onSubscribe(subscriber)
    }
  }

  object IsEvent {
    def unapply(message: String) = {
      val json = global.JSON.parse(message)

      json.event.asInstanceOf[String] match {
        case "line" => Some(Line(
          value = json.value.asInstanceOf[String],
          timestamp = json.timestamp.asInstanceOf[Number].longValue()
        ))
        case "error" =>
          val errorType = json.`type`.asInstanceOf[String]
          val message = json.message.asInstanceOf[String]
          throw new SimpleWebSocketClient.Exception(s"Server-side error throw - $errorType: $message")
        case _ => None
      }
    }
  }

}

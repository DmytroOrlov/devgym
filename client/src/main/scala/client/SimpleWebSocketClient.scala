package client

import monifu.concurrent.Scheduler
import monifu.reactive.OverflowStrategy.Synchronous
import monifu.reactive._
import monifu.reactive.channels.PublishChannel
import org.scalajs.dom.raw.MessageEvent
import org.scalajs.dom.{CloseEvent, ErrorEvent, Event, WebSocket}

import scala.concurrent.Future
import scala.concurrent.duration._

final class SimpleWebSocketClient(url: String, os: Synchronous, sendOnOpen: => Option[String]) extends Observable[String] {
  self =>

  private def createChannel(webSocket: WebSocket)(implicit s: Scheduler) = try {
    val channel = PublishChannel[String](os)
    webSocket.onopen = (event: Event) => sendOnOpen.fold(()) { s =>
      webSocket.send(s)
    }

    webSocket.onerror = (event: ErrorEvent) =>
      channel.pushError(SimpleWebSocketClient.Exception(event.message))

    webSocket.onclose = (event: CloseEvent) =>
      channel.pushComplete()

    webSocket.onmessage = (event: MessageEvent) =>
      channel.pushNext(event.data.asInstanceOf[String])

    channel
  } catch {
    case e: Throwable => Observable.error(e)
  }

  private def closeConnection(webSocket: WebSocket)(implicit s: Scheduler): Unit = {
    if (webSocket != null && webSocket.readyState <= 1)
      try webSocket.close() catch {
        case _: Throwable => ()
      }
  }

  def onSubscribe(subscriber: Subscriber[String]) = {
    import subscriber.scheduler

    var webSocket: WebSocket = null
    val channel = try {
      println(s"Connecting to $url")
      webSocket = new WebSocket(url)
      createChannel(webSocket)
    } catch {
      case e: Throwable => Observable.error(e)
    }

    val source = channel.timeout(15.seconds)
      .doOnCanceled(closeConnection(webSocket))

    source.onSubscribe(new Observer[String] {
      def onNext(elem: String): Future[Ack] =
        subscriber.onNext(elem)

      def onError(e: Throwable) = {
        closeConnection(webSocket)
        scheduler.reportFailure(e)
      }

      def onComplete() =
        closeConnection(webSocket)
    })
  }
}

object SimpleWebSocketClient {

  case class Exception(msg: String) extends RuntimeException(msg)

}

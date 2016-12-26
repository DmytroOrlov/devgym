package client

import monifu.concurrent.Scheduler
import monifu.reactive.OverflowStrategy.Synchronous
import monifu.reactive._
import monifu.reactive.channels.PublishChannel
import org.scalajs.dom.raw.MessageEvent
import org.scalajs.dom.{CloseEvent, ErrorEvent, Event, WebSocket}

import scala.concurrent.duration._
import scala.language.postfixOps
import scala.scalajs.js
import scala.scalajs.js.Any

final class SimpleWebSocketClient(url: String,
                                  os: Synchronous,
                                  sendOnOpen: => Option[js.Any] = None,
                                  timeout: FiniteDuration = 15.seconds,
                                  throttleDuration: Option[FiniteDuration] = None) extends Observable[String] {
  self =>

  private var thisWebSocket: Option[WebSocket] = None

  def webSocket: Option[WebSocket] = thisWebSocket

  def onSubscribe(subscriber: Subscriber[String]) = {
    import subscriber.scheduler

    val (channel, webSocket: Option[WebSocket]) = try {
      val webSocket = new WebSocket(url)
      thisWebSocket = Some(webSocket) //TODO: try to move socket creation to constructor
      createChannel(webSocket) -> Some(webSocket)
    } catch {
      case e: Throwable => Observable.error(e) -> None
    }

    val source: Observable[String] = channel.timeout(timeout)
      .doOnCanceled(webSocket foreach closeConnection)

    source.onSubscribe(new Observer[String] {
      def onNext(elem: String) = subscriber.onNext(elem)

      def onError(ex: Throwable) = {
        webSocket.foreach(closeConnection)
        scheduler.reportFailure(ex)
        subscriber.onComplete()
      }

      def onComplete() = {
        webSocket.foreach(closeConnection)
        subscriber.onComplete()
      }
    })
  }

  private def closeConnection(webSocket: WebSocket)(implicit s: Scheduler) = {
    if (webSocket.readyState <= 1)
      try webSocket.close() catch {
        case _: Throwable => ()
      }
  }

  private def createChannel(webSocket: WebSocket)(implicit s: Scheduler) = try {
    val channel = {
      val simpleChannel = PublishChannel[String](os)
      throttleDuration.map(d => simpleChannel.throttleWithTimeout(d)).getOrElse(simpleChannel)
    }

    webSocket.onopen = (event: Event) => sendOnOpen.foreach(sendEvent)

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

  def sendEvent(s: Any) = {
    webSocket.foreach(ws => ws.send(js.JSON.stringify(s)))
  }
}

object SimpleWebSocketClient {

  case class Exception(msg: String) extends RuntimeException(msg)

}

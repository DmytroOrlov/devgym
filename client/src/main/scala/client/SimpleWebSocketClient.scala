package client

import client.SimpleWebSocketClient.SimpleWebSocketException
import monifu.reactive.OverflowStrategy.Synchronous
import monifu.reactive._
import monifu.reactive.channels.PublishChannel
import org.scalajs.dom.raw.MessageEvent
import org.scalajs.dom.{CloseEvent, ErrorEvent, Event, WebSocket}

import scala.concurrent.duration._
import scala.scalajs.js
import scala.util.Try
import scala.util.control.NonFatal

final class SimpleWebSocketClient(url: String,
                                  os: Synchronous,
                                  sendOnOpen: => Option[js.Any] = None,
                                  timeout: FiniteDuration = 15.seconds) extends Observable[String] {
  def onSubscribe(subscriber: Subscriber[String]): Unit = {
    import subscriber.scheduler

    def inboundWrapper(webSocket: WebSocket) = try {
      val inbound = PublishChannel[String](os)
      webSocket.onopen = (event: Event) => sendOnOpen.foreach(s => webSocket.send(js.JSON.stringify(s)))

      webSocket.onerror = (event: ErrorEvent) =>
        inbound.pushError(SimpleWebSocketException(event.message))

      webSocket.onclose = (event: CloseEvent) =>
        inbound.pushComplete()

      webSocket.onmessage = (event: MessageEvent) =>
        inbound.pushNext(event.data.asInstanceOf[String])

      inbound
    } catch {
      case NonFatal(e) => Observable.error(e)
    }

    val (inbound, closeConnection: (() => Unit)) = try {
      val webSocket = new WebSocket(url)
      inboundWrapper(webSocket) -> (() => if (webSocket.readyState <= 1) Try(webSocket.close()))
    } catch {
      case NonFatal(e) => Observable.error(e) -> ()
    }

    inbound
      .timeout(timeout)
      .doOnCanceled(closeConnection)
      .onSubscribe(new Observer[String] {
        def onNext(elem: String) = subscriber.onNext(elem)

        def onError(ex: Throwable) = {
          closeConnection()
          scheduler.reportFailure(ex)
          subscriber.onComplete()
        }

        def onComplete() = {
          closeConnection()
          subscriber.onComplete()
        }
      })
  }
}

object SimpleWebSocketClient {

  case class SimpleWebSocketException(msg: String) extends RuntimeException(msg)

}

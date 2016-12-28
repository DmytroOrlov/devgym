package client

import client.WebSocketClient.WebSocketClientException
import monifu.reactive.Ack.Continue
import monifu.reactive.OverflowStrategy.DropOld
import monifu.reactive._
import monifu.reactive.channels.PublishChannel
import org.scalajs.dom
import org.scalajs.dom.raw.MessageEvent
import org.scalajs.dom.{CloseEvent, ErrorEvent, Event, WebSocket}

import scala.concurrent.Promise
import scala.concurrent.duration._
import scala.util.control.NonFatal
import scala.util.{Success, Try}

class WebSocketClient(url: String) extends Observable[String] with (String => Unit) {
  private val sendOnOpen = Promise[String => Unit]()

  def apply(message: String): Unit = {
    import scala.concurrent.ExecutionContext.Implicits.global
    sendOnOpen.future.onComplete {
      case Success(send) => send(message)
      case _ => ()
    }
  }

  def onSubscribe(subscriber: Subscriber[String]): Unit = {
    import subscriber.scheduler

    def inboundWrapper(webSocket: WebSocket) = try {
      val inbound = PublishChannel[String](DropOld(2))
      webSocket.onopen = (event: Event) => {
        val outbound = PublishChannel[String](DropOld(2)).sample(Observable.intervalAtFixedRate(100.millis, 1.second))
        outbound.subscribe { e =>
          webSocket.send(e)
          Continue
        }
        sendOnOpen.success(e => outbound.pushNext(e))
      }

      webSocket.onerror = (event: ErrorEvent) =>
        inbound.pushError(WebSocketClientException(event.message))

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

object WebSocketClient {
  def apply(url: String): WebSocketClient = {
    val host = dom.window.location.host
    val protocol = if (dom.document.location.protocol == "https:") "wss:" else "ws:"
    new WebSocketClient(s"$protocol//$host/getSolutionTemplate")
  }

  case class WebSocketClientException(msg: String) extends RuntimeException(msg)

}

package client

import client.WebSocketClient.WebSocketClientException
import monifu.concurrent.Scheduler
import monifu.reactive.OverflowStrategy.DropOld
import monifu.reactive._
import monifu.reactive.channels.PublishChannel
import org.scalajs.dom
import org.scalajs.dom.raw.MessageEvent
import org.scalajs.dom.{CloseEvent, ErrorEvent, Event, WebSocket}

import scala.concurrent.Promise
import scala.util.control.NonFatal
import scala.util.{Success, Try}

class WebSocketClient(url: String) extends Observable[String] with (String => Unit) {

  def apply(message: String): Unit = {
    import scala.concurrent.ExecutionContext.Implicits.global
    sendOnOpen.future.onComplete {
      case Success(send) => send(message)
      case _ => ()
    }
  }

  private val sendOnOpen = Promise[String => Unit]()

  def onSubscribe(subscriber: Subscriber[String]) = {
    def createChannel(webSocket: WebSocket)(implicit s: Scheduler) = try {
      val channel = PublishChannel[String](DropOld(2))
      webSocket.onopen = (event: Event) =>
        sendOnOpen.success(webSocket.send)

      webSocket.onerror = (event: ErrorEvent) =>
        channel.pushError(WebSocketClientException(event.message))

      webSocket.onclose = (event: CloseEvent) =>
        channel.pushComplete()

      webSocket.onmessage = (event: MessageEvent) =>
        channel.pushNext(event.data.asInstanceOf[String])

      channel
    } catch {
      case NonFatal(e) => Observable.error(e)
    }

    import subscriber.scheduler

    val (channel, closeConnection: (() => Unit)) = try {
      val webSocket = new WebSocket(url)
      createChannel(webSocket) -> (() => if (webSocket.readyState <= 1) Try(webSocket.close()))
    } catch {
      case NonFatal(e) => Observable.error(e) -> ()
    }

    channel
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

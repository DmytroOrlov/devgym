package client

import client.SimpleWebSocketClient.SimpleWebSocketException
import monifu.concurrent.Scheduler
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
  private def createChannel(webSocket: WebSocket)(implicit s: Scheduler) = try {
    val channel = PublishChannel[String](os)
    webSocket.onopen = (event: Event) => sendOnOpen.foreach(s => webSocket.send(js.JSON.stringify(s)))

    webSocket.onerror = (event: ErrorEvent) =>
      channel.pushError(SimpleWebSocketException(event.message))

    webSocket.onclose = (event: CloseEvent) =>
      channel.pushComplete()

    webSocket.onmessage = (event: MessageEvent) =>
      channel.pushNext(event.data.asInstanceOf[String])

    channel
  } catch {
    case NonFatal(e) => Observable.error(e)
  }

  def onSubscribe(subscriber: Subscriber[String]) = {
    import subscriber.scheduler

    val (inbound, closeConnection: (() => Unit)) = try {
      val webSocket = new WebSocket(url)
      createChannel(webSocket) -> (() => if (webSocket.readyState <= 1) Try(webSocket.close()))
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

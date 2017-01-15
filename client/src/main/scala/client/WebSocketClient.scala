package client

import client.WebSocketClient.SocketException
import monix.execution.Ack.{Continue, Stop}
import monix.execution.cancelables.AssignableCancelable
import monix.execution.{Ack, Cancelable, FutureUtils}
import monix.reactive.observers.Subscriber
import monix.reactive.{Observable, OverflowStrategy}
import org.scalajs.dom
import org.scalajs.dom.raw.MessageEvent
import org.scalajs.dom.{CloseEvent, ErrorEvent, Event, WebSocket}

import scala.concurrent.Future
import scala.concurrent.duration._
import scala.util.Try
import scala.util.control.NonFatal

class WebSocketClient private(url: String, messages: => Observable[String]) extends Observable[String] {

  def unsafeSubscribeFn(subscriber: Subscriber[String]): Cancelable = {
    val inbound: Observable[String] =
      Observable.create[String](OverflowStrategy.DropOld(100)) { downstream =>
        val cancelOutbound = AssignableCancelable.single()

        try {
          val webSocket = new WebSocket(url)

          def closeConnection(): Unit = {
            cancelOutbound.cancel()
            if (webSocket.readyState <= 1) Try(webSocket.close())
          }

          webSocket.onopen = (event: Event) => {
            implicit val s = subscriber.scheduler
            cancelOutbound := messages.subscribe({ m =>
              webSocket.send(m)
              FutureUtils.delayedResult(1.second)(Continue)
            }, _ => closeConnection(), closeConnection)
          }
          webSocket.onerror = (event: ErrorEvent) => {
            cancelOutbound.cancel()
            downstream.onError(SocketException(event.message))
          }
          webSocket.onclose = (event: CloseEvent) => {
            cancelOutbound.cancel()
            downstream.onComplete()
          }
          webSocket.onmessage = (event: MessageEvent) => {
            val ack = downstream.onNext(event.data.asInstanceOf[String])
            if (ack == Stop) closeConnection()
          }

          Cancelable(closeConnection)
        } catch {
          case NonFatal(ex) =>
            downstream.onError(ex)
            Cancelable.empty
        }
      }

    inbound.unsafeSubscribeFn(subscriber)
  }
}

object WebSocketClient {
  def reconnecting(url: String, messages: => Observable[String]) = {
    val host = dom.window.location.host
    val protocol = if (dom.document.location.protocol == "https:") "wss:" else "ws:"
    new WebSocketClient(s"$protocol//$host/$url", messages) {
      self =>
      override def unsafeSubscribeFn(subscriber: Subscriber[String]): Cancelable =
        super.unsafeSubscribeFn(new Subscriber[String] {
          val scheduler = subscriber.scheduler

          def onNext(elem: String): Future[Ack] =
            subscriber.onNext(elem)

          def onError(ex: Throwable): Unit = {
            scheduler.reportFailure(ex)
            self
              .delaySubscription(3.seconds)
              .unsafeSubscribeFn(subscriber)
          }

          def onComplete(): Unit =
            self
              .delaySubscription(3.seconds)
              .unsafeSubscribeFn(subscriber)
        })
    }
  }

  case class SocketException(msg: String) extends RuntimeException(msg)

}

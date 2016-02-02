package stream

import akka.actor.{Actor, ActorRef, Props}
import monifu.concurrent.Scheduler
import monifu.concurrent.cancelables.CompositeCancelable
import monifu.reactive.Ack.Continue
import monifu.reactive.Observable
import play.api.Logger
import play.api.libs.json.{JsValue, Json, Writes}
import shared.Event

import scala.concurrent.duration._

class SimpleWebSocketActor[T <: Event : Writes](out: ActorRef, producer: String => Observable[T])
                                               (implicit s: Scheduler) extends Actor {
  private[this] val subscription =
    CompositeCancelable()

  def receive: Receive = {
    case solution: String =>
      context.become(next)

      subscription += producer(solution)
        .map(x => Json.toJson(x))
        .timeout(10.seconds)
        .subscribe(
          jsValue => {
            out ! jsValue
            Continue
          },
          e => context.stop(self),
          () => context.stop(self)
        )
  }

  def next: Receive = {
    case _ =>
  }

  override def postStop(): Unit = {
    subscription.cancel()
    super.postStop()
  }

  def serverError(e: Throwable) = {
    Logger.warn(s"Error while serving a web-socket stream", e)
    out ! Json.obj(
      "event" -> "error",
      "type" -> e.getClass.getName,
      "message" -> e.getMessage,
      "timestamp" -> System.currentTimeMillis())
    context.stop(self)
  }
}

object SimpleWebSocketActor {
  /** Utility for quickly creating a `Props` */
  def props[T <: Event : Writes](out: ActorRef, producer: String => Observable[T])
                                (implicit s: Scheduler): Props = {
    Props(new SimpleWebSocketActor(out, producer))
  }

  /**
   * Used in order to not confuse self messages versus those
   * sent from the client.
   */
  case class Next(value: JsValue)

}

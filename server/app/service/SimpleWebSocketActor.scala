package service

import akka.actor.{Actor, ActorRef, Props}
import monifu.concurrent.Scheduler
import monifu.concurrent.cancelables.CompositeCancelable
import monifu.reactive.Ack.Continue
import monifu.reactive.Observable
import monifu.reactive.OverflowStrategy.DropOld
import monifu.reactive.channels.PublishChannel
import play.api.Logger
import play.api.libs.json.{JsValue, Json, Writes}
import shared.{Event, Line}

import scala.concurrent.Future
import scala.concurrent.duration._
import scala.util.{Failure, Success, Try}

class SimpleWebSocketActor[T <: Event : Writes](out: ActorRef, producer: String => Observable[T], onSubscribe: => Option[T], timeout: FiniteDuration)
                                               (implicit s: Scheduler) extends Actor {
  private[this] val subscription =
    CompositeCancelable()

  def receive: Receive = {
    case solution: String =>
      context.become(next)

      subscription += producer(solution)
        .map(x => Json.toJson(x))
        .timeout(timeout)
        .subscribe(
          jsValue => {
            out ! jsValue
            Continue
          },
          e => context.stop(self),
          () => context.stop(self)
        )
      onSubscribe.foreach(out ! Json.toJson(_))
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
  def props[T <: Event : Writes](out: ActorRef, producer: String => Observable[T], onSubscribe: => Option[T] = None, timeout: FiniteDuration = 10.seconds)
                                (implicit s: Scheduler): Props = {
    Props(new SimpleWebSocketActor(out, producer, onSubscribe, timeout))
  }

  /**
   * Used in order to not confuse self messages versus those
   * sent from the client.
   */
  case class Next(value: JsValue)

  def createChannel(execSuite: String => Try[String])(solution: String)(implicit s: Scheduler): Observable[Line] = {
    val channel = PublishChannel[Line](DropOld(20))
    Future {
      val lines = (execSuite(solution) match {
        case Success(v) => v
        case Failure(e) => e.getMessage
      }).split("\n")

      lines.foreach(s => channel.pushNext(Line(s)))
      channel.pushComplete()
    }
    channel
  }
}

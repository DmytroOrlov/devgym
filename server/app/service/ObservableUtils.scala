package service

import monifu.concurrent.Scheduler
import monifu.reactive.OverflowStrategy.DropOld
import monifu.reactive.channels.PublishChannel

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

object ObservableRunner {
  def apply(block: => (String => Unit) => Unit)(implicit s: Scheduler) = {
    val channel = PublishChannel[String](DropOld(20))
    val f = Future(block(s => channel.pushNext(s)))
    f.onComplete {
      case Success(_) => channel.pushComplete()
      case Failure(e) => channel.pushNext(e.getMessage)
        channel.pushComplete()
    }
    channel
  }
}

object StringBuilderRunner {
  def apply(block: => (String => Unit) => Unit)(implicit ec: ExecutionContext): String = {
    val sb = new StringBuilder
    block(s => sb.append(s))
    sb.toString()
  }
}

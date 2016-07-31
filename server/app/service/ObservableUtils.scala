package service

import monifu.concurrent.Scheduler
import monifu.reactive.OverflowStrategy.DropOld
import monifu.reactive.channels.PublishChannel

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

object ObservableRunner {
  def apply(block: => (String => Unit) => String,
            testStatus: Either[Throwable, String] => Option[String] = {_ => None})
           (implicit s: Scheduler) = {

    val channel = PublishChannel[String](DropOld(20))

    def completeTest(result: Either[Throwable, String]) = {
      testStatus(result).foreach(channel.pushNext(_))
      channel.pushComplete()
    }

    val f = Future(block(s => channel.pushNext(s)))
    f.onComplete {
      case Success(r) =>
        completeTest(Right(r))
      case Failure(e) =>
        completeTest(Left(e))
    }
    channel
  }
}

object StringBuilderRunner {
  def apply(block: => (String => Unit) => String)(implicit ec: ExecutionContext): String = {
    val sb = new StringBuilder
    block(s => sb.append(s))
    sb.toString()
  }
}

package service

import monifu.concurrent.Scheduler
import monifu.reactive.OverflowStrategy.DropOld
import monifu.reactive.channels.PublishChannel
import shared.model.{Event, Line, TestResult}

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Success, Try}

object ObservableRunner {

  def apply(block: => (String => Unit) => String,
            testResult: Try[String] => TestResult)
           (implicit s: Scheduler): PublishChannel[Event] = {

    val channel = PublishChannel[Event](DropOld(20))

    def pushTestResult(result: Try[String]) = {
      channel.pushNext(testResult(result))
      channel.pushComplete()
    }

    Future(block(s => channel.pushNext(Line(s)))).onComplete(pushTestResult)
    channel
  }
}

object StringBuilderRunner {
  def apply(block: => (String => Unit) => String, testResult: Try[String] => Option[TestResult] = { _ => None})
           (implicit ec: ExecutionContext): String = {
    val sb = new StringBuilder
    block(s => sb.append(s))
    testResult(Success(sb.toString())).map(_.testStatus.toString).foreach(sb.append)
    sb.toString()
  }
}

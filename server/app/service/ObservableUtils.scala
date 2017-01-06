package service

import monix.execution.{Cancelable, Scheduler}
import monix.reactive.{Observable, OverflowStrategy}
import shared.model.{Event, Line, TestResult}

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Success, Try}

object ObservableRunner {

  def apply(block: => (String => Unit) => String,
            testResult: Try[String] => TestResult)
           (implicit s: Scheduler): Observable[Event] = {

    Observable.create[Event](OverflowStrategy.DropOld(20)) { downstream =>
      def pushTestResult(result: Try[String]) = {
        downstream.onNext(testResult(result))
        downstream.onComplete()
      }

      Future(block(s => downstream.onNext(Line(s)))).onComplete(pushTestResult)
      Cancelable.empty
    }
  }
}

object StringBuilderRunner {
  def apply(block: => (String => Unit) => String,
            testResult: Try[String] => Option[TestResult] = { _ => None })
           (implicit ec: ExecutionContext): String = {

    val sb = new StringBuilder
    block(s => sb.append(s))

    testResult(Success(sb.toString()))
      .map(_.testStatus.toString)
      .foreach(sb.append)

    sb.toString()
  }
}

package service

import monix.eval.Task
import monix.execution.Scheduler
import monix.reactive.{Observable, OverflowStrategy}
import shared.model.{Event, Line, TestResult}

import scala.concurrent.ExecutionContext
import scala.util.{Success, Try}

object ObservableRunner {

  def apply(block: => (String => Unit) => Unit,
            testAsync: PushResult => (CheckNext, OnBlockComplete))
           (implicit s: Scheduler): Observable[Event] = {

    Observable.create[Event](OverflowStrategy.DropOld(20)) { downstream =>
      val (checkNext, onBlockComplete) = testAsync { testResult =>
        downstream.onNext(testResult)
        downstream.onComplete()
      }
      Task(block { next =>
        downstream.onNext(Line(next))
        checkNext(next)
      }).runAsync(onBlockComplete)
    }
  }
}

object StringBuilderRunner {
  def apply(block: (String => Unit) => Unit,
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

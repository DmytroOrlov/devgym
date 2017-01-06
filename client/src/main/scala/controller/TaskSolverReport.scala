package controller

import monix.execution.Ack
import monix.execution.Ack.Continue
import monix.reactive.Observer
import org.scalajs.jquery.{JQuery, jQuery}
import shared.model._
import shared.view.SuiteReportUtil.{removeToolboxText, replaceMarkers}

import scala.concurrent.Future

final class TaskSolverReport(reportId: String, onCompleteCall: () => Unit) extends Observer[Event] {
  private var compilationJustStarted = false

  private val report = jQuery(s"#$reportId")
  report.empty()

  override def onNext(elem: Event): Future[Ack] = {
    elem match {
      case l: Line => processLine(l)
      case tr: TestResult => processTestResult(tr)
      case _: Compiling => processCompiling()
      case _ => System.err.println(s"Event '$elem' is not supported")
    }
    Continue
  }

  private def processCompiling(): Unit = {
    report.append("Compiling...")
    compilationJustStarted = true
  }

  private def processTestResult(tr: TestResult) = {
    cleanReportAreaIfNeeded()

    val (message, cssClass) = tr.testStatus match {
      case TestStatus.Passed => ("Test Passed!", "testPassed")
      case TestStatus.FailedByTest | TestStatus.FailedByCompilation =>
        val error = Option(tr.errorMessage).filter(_.nonEmpty).map(_ + "<br/>").getOrElse("")
        (s"${error}Test Failed. Keep going!", "testFailed")
    }
    report.append(s"""<div class="$cssClass">$message</div>""")
  }

  private def processLine(l: Line): JQuery = {
    val line = removeToolboxText(replaceMarkers(l.value))
    cleanReportAreaIfNeeded()
    report.append(s"""<div>$line</div>""")
  }

  private def cleanReportAreaIfNeeded(): Unit = {
    if (compilationJustStarted) {
      compilationJustStarted = false
      report.html("<div class='result-output'>Result:</div>")
    }
  }

  def onComplete() = {
    onCompleteCall()
  }

  def onError(ex: Throwable) = {
    val m = s"${this.getClass.getName} $ex"
    System.err.println(m)
    report.append(m)
    onCompleteCall()
  }
}


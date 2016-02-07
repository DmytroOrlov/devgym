package service

import java.io.OutputStream

import monifu.reactive.{Channel, Observable}
import org.scalatest.Suite
import service.ScalaTestRunner._
import shared.Line

import scala.concurrent.{ExecutionContext, Future}
import scala.util.control.NonFatal

trait SuiteExecution {
  /**
   * Executes tests in this <code>Suite</code>, printing output to result string.
   * Should not throw exception.
   */
  def executionOutput(channel: Channel[Line], suiteInstance: Suite) = {
    val sb = new StringBuilder()
    val s = new OutputStream() {
      override def write(b: Array[Byte], off: Int, len: Int) =
        if (len != 0) {
          val s = new String(b, off, len)
          channel.pushNext(Line(s))
          sb.append(s)
        }

      override def write(b: Int): Unit = channel.pushNext(Line(b.toChar.toString))
    }
    Console.withOut(s)(suiteInstance.execute(color = false))
    channel.pushComplete()
    sb.toString()
  }
}

object FailWithMessage {
  def apply[B](msg: String)(block: => B): B =
    try block catch {
      case NonFatal(e) => throw new SuiteException(msg + e.getMessage)
    }
}

trait SuiteToolbox {
  val failed = "FAILED"
  val failedInRuntime = "failed in runtime"
  val userClass = "UserSolution"
  val classDefPattern = """class\s*([\w\$]*)""".r
  val traitDefPattern = """trait\s*([\w\$]*)""".r
  val defaultImports = "import org.scalatest._"
  val tb = {
    import scala.reflect.runtime._
    import scala.tools.reflect.ToolBox
    val cm = universe.runtimeMirror(getClass.getClassLoader)
    cm.mkToolBox()
  }
}

trait DynamicExecution extends SuiteExecution with SuiteToolbox {
  private def findSuitName(suite: String) = classDefPattern.findFirstIn(suite).get.split( """\s+""")(1)

  def executeDynamic(channel: Channel[Line], suite: String, patchedSolution: String) = {
    val suiteName = FailWithMessage("There is no Test Suite name to instantiate, code: ") {
      findSuitName(suite)
    }
    val runningCode = s"$defaultImports; $suite; $patchedSolution; new $suiteName(new $userClass)"
    executionOutput(channel, suiteInstance = tb.eval(tb.parse(runningCode)).asInstanceOf[Suite])
  }
}

trait CheckSuite {
  def validate: String => Boolean

  def check(in: (Observable[Line], Future[String]))(implicit ec: ExecutionContext): (Observable[Line], Future[String]) = in match {
    case (o, f) => o -> f.filter(validate)
  }
}

object ObservableFuture {

  implicit class RichObservableFuture(val of: (Observable[Line], Future[String])) extends AnyVal {
    def observable = of._1

    def future = of._2
  }

}

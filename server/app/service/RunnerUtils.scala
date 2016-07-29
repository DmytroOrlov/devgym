package service

import java.io.OutputStream

import org.scalatest.Suite

import scala.util.control.NonFatal

trait SuiteExecution {
  /**
   * Executes tests in this <code>Suite</code>.
   * @param channel is meant for imperative style feeding of output events.
   */
  def executionOutput(suiteInstance: Suite, channel: String => Unit): Unit = {
    val s = new OutputStream() {
      override def write(b: Array[Byte], off: Int, len: Int) =
        if (len != 0) channel(new String(b, off, len))

      override def write(b: Int): Unit = channel(b.toChar.toString)
    }
    try {
      //scalatest entry point - execute()
      Console.withOut(s)(suiteInstance.execute())
    } finally s.close()
  }
}

object WithSuiteException {
  def apply[B](msg: String)(block: => B): B =
    try block catch {
      case e: SuiteException => throw e
      case NonFatal(e) => throw SuiteException(msg, Some(e))
    }
}

trait SuiteToolbox {
  val failed = "FAILED"
  val userClass = "UserSolution"
  val classDefPattern = """class\s*([\w\$]*)""".r
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

  def executeDynamic(suite: String, patchedSolution: String, channel: String => Unit) = {
    val suiteName = WithSuiteException(s"There is no Test Suite name to instantiate, code: $suite") {
      findSuitName(suite)
    }
    val runningCode = s"$defaultImports; $suite; $patchedSolution; new $suiteName(new $userClass)"
    executionOutput(suiteInstance = tb.eval(tb.parse(runningCode)).asInstanceOf[Suite], channel)
  }
}

case class SuiteException(msg: String, e: Option[Throwable] = None) extends RuntimeException(msg)

package service.reflection

import java.io.OutputStream

import org.scalatest.Suite

import scala.util.control.NonFatal

trait SuiteExecution {
  /**
    * Executes tests in this <code>Suite</code>.
    *
    * @param channel is used to pass the test result to
    */
  def executionTestSuite(suite: Suite, channel: String => Unit): String = {
    val result = new StringBuilder
    val s = new OutputStream() {
      override def write(b: Array[Byte], off: Int, len: Int) =
        if (len != 0) inChannel(new String(b, off, len))

      override def write(b: Int): Unit = inChannel(b.toChar.toString)

      def inChannel(s: String) = {
        channel(s)
        result.append(s)
      }
    }
    try {
      //execute() - ScalaTest entry point
      Console.withOut(s)(suite.execute())
      result.toString
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

  def executeDynamic(suite: String, patchedSolution: String, channel: String => Unit): String = {
    val suiteName = WithSuiteException(s"There is no Test Suite name to instantiate, code: $suite") {
      findSuitName(suite)
    }
    val runningCode = s"$defaultImports; $suite; $patchedSolution; new $suiteName(new $userClass)"
    executionTestSuite(suite = tb.eval(tb.parse(runningCode)).asInstanceOf[Suite], channel)
  }
}

case class SuiteException(msg: String, e: Option[Throwable] = None) extends RuntimeException(msg)

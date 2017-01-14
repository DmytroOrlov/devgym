package service.reflection

import java.io.OutputStream

import org.scalatest.Suite

import scala.util.control.NonFatal

trait SuiteExecution {
  /**
    * Executes tests in this <code>Suite</code>.
    *
    * @param channel is used for imperative style feeding of output events (test result)
    */
  def executionTestSuite(suite: Suite, channel: String => Unit): Unit = {
    val s = new OutputStream() {
      override def write(b: Array[Byte], off: Int, len: Int) =
        if (len != 0) channel(new String(b, off, len))

      override def write(b: Int): Unit = channel(b.toChar.toString)
    }
    try {
      //execute() - ScalaTest entry point
      Console.withOut(s)(suite.execute())
    } finally s.close()
  }
}

trait SuiteToolbox {
  val userClass = "UserSolution"
  val classDefPattern = """class\s*([\w\$]*)""".r
  val defaultImports = "import org.scalatest._"
  val tb = {
    import scala.reflect.runtime._
    import scala.tools.reflect.ToolBox
    val cm = universe.runtimeMirror(new TaskClassLoader(getClass.getClassLoader))
    cm.mkToolBox()
  }
}

trait DynamicExecution extends SuiteExecution with SuiteToolbox {
  private def findSuitName(suite: String) = classDefPattern.findFirstIn(suite).get.split( """\s+""")(1)

  def executeDynamic(suite: String, patchedSolution: String, channel: String => Unit): Unit = {
    val suiteName = findSuitName(suite)
    val code = s"$defaultImports;\n $suite; $patchedSolution;\n new $suiteName(new $userClass)"
    executionTestSuite(suite = tb.eval(tb.parse(code)).asInstanceOf[Suite], channel)
  }
}

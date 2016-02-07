package service

import java.io.ByteArrayOutputStream

import org.scalatest.Suite
import service.ScalaTestRunner._

import scala.util.{Failure, Success, Try}

trait SuiteExecution {
  /**
   * Executes tests in this <code>Suite</code>, printing output to result string.
   * Should not throw exception.
   */
  def executionOutput(suiteInstance: Suite): String = {
    val o = new ByteArrayOutputStream
    Console.withOut(o)(suiteInstance.execute())
    o.toString
  }
}

trait TryBlock {
  def failurePrefix(s: String): String

  def tryBlock(msg: String = "")(block: => String): Try[String] =
    Try(block).transform(
      v => Success(v),
      e => Failure(new SuiteException(s"${failurePrefix(msg)}${e.getMessage}"))
    )
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
  private val t = new TryBlock {
    override def failurePrefix(s: String): String = s"There is no Test Suite name to instantiate, code: $s"
  }

  private def findSuitName(suite: String): Try[String] =
    t.tryBlock(suite) {
      classDefPattern.findFirstIn(suite).get.split( """\s+""")(1)
    }

  def executeDynamic(suite: String, patchedSolution: String): Try[String] =
    findSuitName(suite).map { suiteName =>
      val runningCode = s"$defaultImports; $suite; $patchedSolution; new $suiteName(new $userClass)"
      executionOutput(suiteInstance = tb.eval(tb.parse(runningCode)).asInstanceOf[Suite])
    }
}

package service

import java.io.ByteArrayOutputStream

import org.scalatest.Suite
import service.ScalaTestRunner._

import scala.util.{Failure, Success, Try}

trait ExecuteSuite {
  /**
   * Executes tests in this <code>Suite</code>, printing output to result string.
   * Should not throw exception.
   */
  def executionOutput(suiteInstance: Suite): String = {
    val o = new ByteArrayOutputStream
    Console.withOut(o)(suiteInstance.execute(color = false))
    o.toString /*match {
      case s if s.contains(failedMarker) => Failure(new SolutionException(s"contains $failedMarker"))
      case s => Success(s)
    }*/
  }
}

trait TryBlock {
  def failurePrefix(s: String): String

  def tryBlock(msg: String = "")(block: => String): Try[String] = apply(msg)(block)

  def apply(msg: String)(block: => String): Try[String] =
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

trait ExecuteDynamic extends ExecuteSuite with SuiteToolbox {
  private val trySuite = new TryBlock {
    override def failurePrefix(s: String): String = s"There is no Test Suite name to instantiate, code: $s"
  }

  private def findSuitName(suite: String): Try[String] =
    trySuite(suite) {
      classDefPattern.findFirstIn(suite).get.split( """\s+""")(1)
    }

  def executeDynamic(suite: String, patchedSolution: String): Try[String] =
    findSuitName(suite).map { suiteName =>
      val runningCode = s"$defaultImports; $suite; $patchedSolution; new $suiteName(new $userClass)"
      executionOutput(suiteInstance = tb.eval(tb.parse(runningCode)).asInstanceOf[Suite])
    }
}

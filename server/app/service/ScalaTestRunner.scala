package service

import java.io.ByteArrayOutputStream

import org.scalatest.Suite

import scala.util.control.NonFatal


/**
 * Runs test suite of scalatest library using the 'execute' method
 */
object ScalaTestRunner {
  val failedMarker = "FAILED"
  val failedInRuntimeMarker = "failed in runtime"
  val userClass = "UserSolution"
  val classDefPattern = """class\s*([\w\$]*)""".r
  val traitDefPattern = """trait\s*([\w\$]*)""".r
  val defaultImports = "import org.scalatest._"

  import scala.reflect.runtime._
  val cm = universe.runtimeMirror(getClass.getClassLoader)
  import scala.tools.reflect.ToolBox
  val tb = cm.mkToolBox()

  /**
   * Runs suite loaded in runtime with dynamic solution
   */
  def tryExecSuite(solution: String, suiteClass: Class[Suite], solutionTrait: Class[AnyRef]): String = tryExec {
    val solutionInstance = createSolutionInstance(solution, solutionTrait)
    execSuite(suiteClass.getConstructor(solutionTrait).newInstance(solutionInstance))
  }

  /**
   * Runs dynamic solution and dynamic suite
   */
  def tryExecSuite(solution: String, suite: String): String = tryExec(execSuite(solution, suite))

  def execSuite(solution: String, suite: String): String = {
    val runningCode = try {
      classDefPattern.findFirstIn(suite) match {
        case Some(v) =>
          val suiteName = v.split( """\s+""")(1)
          val patchedSolution = classDefPattern.replaceFirstIn(solution, s"class $userClass ")
          s"$defaultImports; $suite; $patchedSolution; new $suiteName(new $userClass)"
        case None => throw new SolutionException(s"There is no Test Suite name to instantiate, code: $suite")
      }
    } catch {
      case e: SolutionException => throw e
      case NonFatal(e) => throw new SolutionException(e.getMessage)
    }
    execSuite(suiteInstance = tb.eval(tb.parse(runningCode)).asInstanceOf[Suite])
  }

  /**
   * Runs suite instance with solution instance
   */
  def execSuite(suiteInstance: Suite): String = new ByteArrayOutputStream { stream =>
    Console.withOut(stream) {
      suiteInstance.execute(color = false)
    }
  }.toString

  private def tryExec(suite: => String) =
    try suite catch {
      case e: SolutionException => throw e
      case NonFatal(e) => s"Test $failedInRuntimeMarker with error:\n${e.getMessage}'"
    }

  def createSolutionInstance(solution: String, solutionTrait: Class[AnyRef]): AnyRef = {
    val patchedSolution = classDefPattern.replaceFirstIn(solution, s"class $userClass extends ${solutionTrait.getSimpleName} ")
    val dynamicCode = s"import ${solutionTrait.getName}; $patchedSolution; new $userClass"

    tb.eval(tb.parse(dynamicCode)).asInstanceOf[AnyRef]
  }

  case class SolutionException(msg: String) extends RuntimeException(msg)

}

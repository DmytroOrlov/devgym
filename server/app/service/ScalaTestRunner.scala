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
  def execSuite(solution: String, suiteClass: Class[Suite], solutionTrait: Class[AnyRef]): String =
    tryExecSuite {
      val solutionInstance = createSolutionInstance(solution, solutionTrait)
      execSuite(suiteClass.getConstructor(solutionTrait).newInstance(solutionInstance))
    }

  /**
   * Runs dynamic solution and dynamic suite
   */
  def execSuite(solution: String, suite: String): String = {
    //todo: solutionTrait should be taken from DB and populated during the task creation by user
    val solutionTrait = traitDefPattern.findFirstIn(suite) match {
      case Some(v) => v.split( """\s+""")(1)
      case None => throw new SolutionException(s"There is no trait type defined in the Test constructor, code: $suite")
    }

    val suiteName = classDefPattern.findFirstIn(suite) match {
      case Some(v) => v.split( """\s+""")(1)
      case None => throw new SolutionException(s"There is no Test Suite name to instantiate, code: $suite")
    }

    val patchedSolution = classDefPattern.replaceFirstIn(solution, s"class $userClass extends $solutionTrait ")
    val runningCode = s"$defaultImports; $suite; $patchedSolution; new $suiteName(new $userClass)"

    tryExecSuite {
      execSuite(suiteInstance = tb.eval(tb.parse(runningCode)).asInstanceOf[Suite])
    }
  }

  /**
   * Runs suite instance with solution instance
   */
  def execSuite(suiteInstance: Suite): String = new ByteArrayOutputStream { stream =>
    Console.withOut(stream) {
      suiteInstance.execute(color = false)
    }
  }.toString

  private def tryExecSuite(execution: => String) =
    try execution catch {
      case NonFatal(e) => s"Test $failedInRuntimeMarker with error:\n${e.getMessage}'"
    }

  private def createSolutionInstance(solution: String, solutionTrait: Class[AnyRef]): AnyRef = {
    val patchedSolution = classDefPattern.replaceFirstIn(solution, s"class $userClass extends ${solutionTrait.getSimpleName} ")
    val dynamicCode = s"import ${solutionTrait.getName}; $patchedSolution; new $userClass"

    tb.eval(tb.parse(dynamicCode)).asInstanceOf[AnyRef]
  }

  case class SolutionException(msg: String) extends RuntimeException(msg)

}

package service

import java.io.ByteArrayOutputStream

import org.scalatest.Suite

import scala.util.{Failure, Success, Try}


/**
  * Runs test suite of scalatest library using the 'execute' method
  */
object ScalaTestRunner {
  val failedMarker = "FAILED"
  val failedInRuntimeMarker = "failed in runtime"
  val userClass = "UserSolution"
  val classDefPattern = "(class [A-Za-z0-9]* )".r //todo add $ _ characters
  val traitDefPattern = "(trait [A-Za-z0-9]* )".r //todo add $ _ characters
  val defaultImports = "import org.scalatest._"

  import scala.reflect.runtime._
  val cm = universe.runtimeMirror(getClass.getClassLoader)
  import scala.tools.reflect.ToolBox
  val tb = cm.mkToolBox()

  def execSuite(solution: String, suiteClass: Class[Suite], solutionTrait: Class[AnyRef]): String = {
    def execution() = {
      val solutionInstance = createSolutionInstance(solution, solutionTrait)
      execSuite(suiteClass.getConstructor(solutionTrait).newInstance(solutionInstance))
    }
    tryExecSuite(execution())
  }

  def execSuite(solution: String, suite: String): String = {
    val traitDefPattern(solutionTrait) = suite
    val classDefPattern(suiteName) = suite

    val patchedSolution = classDefPattern.replaceFirstIn(solution, s"class $userClass extends $solutionTrait ")
    val runningCode = s"$defaultImports; $suite; $patchedSolution; new $suiteName(new $userClass)"
    val suiteRef = tb.eval(tb.parse(runningCode)).asInstanceOf[Suite]

    tryExecSuite(execSuite(suiteRef))
  }

  def execSuite(suiteInstance: Suite): String = new ByteArrayOutputStream {stream =>
    Console.withOut(stream) {
      suiteInstance.execute(color = false)
    }
  }.toString

  private def tryExecSuite(execution: => String): String = {
    Try {
      execution
    } match {
      case Success(s) => s
      case Failure(e) => s"Test $failedInRuntimeMarker with error:\n${e.getMessage}'"
    }
  }

  private def createSolutionInstance(solution: String, solutionTrait: Class[AnyRef]): AnyRef = {
    val patchedSolution = classDefPattern.replaceFirstIn(solution, s"class $userClass extends ${solutionTrait.getSimpleName} ")
    val dynamicCode = s"import ${solutionTrait.getName}; $patchedSolution; new $userClass"

    tb.eval(tb.parse(dynamicCode)).asInstanceOf[AnyRef]
  }
}

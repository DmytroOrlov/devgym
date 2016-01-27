package service

import java.io.ByteArrayOutputStream

import org.scalatest.Suite

import scala.util.{Failure, Success, Try}


/**
  * Runs scalatest library test suite using the 'execute' method
  */
object ScalaTestRunner {
  val failedMarker = "FAILED"
  val failedInRuntimeMarker = "failed in runtime"
  val userClass = "UserSolution"

  def execSuite(solution: String, suiteClass: Class[Suite], solutionTrait: Class[AnyRef]): String = {
    Try {
      val solutionInstance = createSolutionInstance(solution, solutionTrait)
      execSuite(suiteClass.getConstructor(solutionTrait).newInstance(solutionInstance))
    } match {
      case Success(s) => s
      case Failure(e) => s"Test failed in runtime with error:\n${e.getMessage}'"
    }
  }

  def execSuite(suiteInstance: Suite): String = {
    val stream = new ByteArrayOutputStream

    Console.withOut(stream) {
      suiteInstance.execute(color = false)
    }

    stream.toString
  }

  private def createSolutionInstance(solution: String, solutionTrait: Class[AnyRef]): AnyRef = {
    import scala.reflect.runtime._
    val cm = universe.runtimeMirror(getClass.getClassLoader)
    import scala.tools.reflect.ToolBox
    val tb = cm.mkToolBox()

    val patchedSolution = solution.replaceFirst("(class [A-Za-z0-9]* )", s"class $userClass extends ${solutionTrait.getSimpleName} ")
    val dynamicCode = s"import ${solutionTrait.getName}; $patchedSolution; new $userClass"
    tb.eval(tb.parse(dynamicCode)).asInstanceOf[AnyRef]
  }
}
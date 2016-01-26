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

  def execSuite(solution: String, suiteClass: Class[Suite], solutionTrait: Class[AnyRef]): String = {
    Try {
      val solutionInstance = createSolutionInstance(solution, solutionTrait)
      execSuite(suiteClass.getConstructor(solutionTrait).newInstance(solutionInstance))
    } match {
      case Success(s) => s
      case Failure(e) => s"Test failed in runtime with error:\n${e.getMessage}'"
    }
  }

  def execSuite(suiteInstance: Suite) = {
    val stream = new ByteArrayOutputStream

    Console.withOut(stream) {
      suiteInstance.execute(stats = true, fullstacks = true, durations = true)
    }

    stream.toString
  }

  private def createSolutionInstance(solution: String, solutionTrait: Class[AnyRef]): AnyRef = {
    import scala.reflect.runtime._
    val cm = universe.runtimeMirror(getClass.getClassLoader)
    import scala.tools.reflect.ToolBox
    val tb = cm.mkToolBox()

    val dynamicCode = s"import ${solutionTrait.getName}; new ${solutionTrait.getSimpleName} {$solution}"
    val solutionInstance = tb.eval(tb.parse(dynamicCode)).asInstanceOf[AnyRef]
    solutionInstance
  }
}
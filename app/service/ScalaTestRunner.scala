package service

import java.io.ByteArrayOutputStream

import org.scalatest.Suite

import scala.util.{Failure, Success, Try}


/**
  * Runs scalatest library test suite using the 'execute' method
  */
object ScalaTestRunner {

  def execSuite(solution: String, suiteClass: Class[Suite], solutionTrait: Class[AnyRef]): String = {
    Try {
      val solutionInstance = createSolutionInstance(solution, solutionTrait)
      val stream = new ByteArrayOutputStream

      Console.withOut(stream) {
        suiteClass.getConstructor(solutionTrait).newInstance(solutionInstance).execute(stats = true, fullstacks = true, durations = true)
      }

      stream.toString
    } match {
      case Success(s) => s
      case Failure(e) => s"Test failed with error:\n${e.getMessage}'"
    }
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

//TODO: move to unit tests & delete below code
object ScalaTestRunnerApp extends App {
  val report = ScalaTestRunner.execSuite(
    "def sleepIn(weekday: Boolean, vacation: Boolean): Boolean = {!weekday || vacation}",
    Class.forName("tasktest.SleepInTest").asInstanceOf[Class[Suite]],
    Class.forName("tasktest.SleepInSolution").asInstanceOf[Class[AnyRef]]
  )
  println(report)
}
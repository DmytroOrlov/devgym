package service

import java.io.ByteArrayOutputStream

import org.scalatest.Suite
import tasktest.{SleepInSolution, SleepInTest}


/**
  * Runs scalatest library test suite using the 'execute' method
  */
class ScalaTestRunner {

  def execSuite(solution: String, suiteClass: Class[Suite], solutionTrait: Class[AnyRef]): String = {
    val solutionInstance = createSolutionInstance(solution, solutionTrait)
    val stream = new ByteArrayOutputStream

    Console.withOut(stream) {
      suiteClass.getConstructor(solutionTrait).newInstance(solutionInstance).execute(stats = false)
    }

    stream.toString
  }

  def createSolutionInstance(solution: String, solutionTrait: Class[AnyRef]): AnyRef = {
    import scala.reflect.runtime._
    val cm = universe.runtimeMirror(getClass.getClassLoader)
    import scala.tools.reflect.ToolBox
    val tb = cm.mkToolBox()
    val dynamicCode = s"import ${solutionTrait.getName}; new ${solutionTrait.getSimpleName} {$solution}"
    val solutionInstance = tb.eval(tb.parse(dynamicCode)).asInstanceOf[AnyRef]
    solutionInstance
  }
}

object ScalaTestRunnerApp extends App {
  val runner = new ScalaTestRunner
  val report = runner.execSuite(
    "def sleepIn(weekday: Boolean, vacation: Boolean): Boolean = {!weekday || vacation}",
    classOf[SleepInTest].asInstanceOf[Class[Suite]],
    classOf[SleepInSolution].asInstanceOf[Class[AnyRef]]
  )
  println(report)
}
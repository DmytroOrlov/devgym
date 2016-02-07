package service

import monifu.concurrent.Scheduler
import monifu.reactive.Observable
import monifu.reactive.OverflowStrategy.DropOld
import monifu.reactive.channels.PublishChannel
import org.scalatest.Suite
import shared.Line

import scala.concurrent.Future
import scala.util.control.NonFatal

trait RuntimeSuiteExecutor extends CheckSuite {
  def apply(suiteClass: Class[Suite], solutionTrait: Class[AnyRef])
           (solution: String)
           (implicit s: Scheduler): (Observable[Line], Future[String])
}

trait ScalaRuntimeRunner extends RuntimeSuiteExecutor with SuiteExecution with SuiteToolbox {
  /**
   * Runs suite loaded in runtime with dynamic solution
   */
  def apply(suiteClass: Class[Suite], solutionTrait: Class[AnyRef])
           (solution: String)
           (implicit s: Scheduler): (Observable[Line], Future[String]) = {
    val channel = PublishChannel[Line](DropOld(20))
    val f = Future {
      val solutionInstance = createSolutionInstance(solution, solutionTrait)
      executionOutput(channel, suiteClass.getConstructor(solutionTrait).newInstance(solutionInstance))
    }
    f.onFailure {
      case NonFatal(e) => channel.pushNext(Line(e.getMessage))
        channel.pushComplete()
    }
    channel -> f
  }

  def createSolutionInstance(solution: String, solutionTrait: Class[AnyRef]): AnyRef = {
    val patchedSolution = classDefPattern.replaceFirstIn(solution, s"class $userClass extends ${solutionTrait.getSimpleName} ")
    val dynamicCode = s"import ${solutionTrait.getName}; $patchedSolution; new $userClass"

    tb.eval(tb.parse(dynamicCode)).asInstanceOf[AnyRef]
  }
}

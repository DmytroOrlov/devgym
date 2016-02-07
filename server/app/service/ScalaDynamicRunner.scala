package service

import monifu.concurrent.Scheduler
import monifu.reactive.Observable
import monifu.reactive.OverflowStrategy.DropOld
import monifu.reactive.channels.PublishChannel
import shared.Line

import scala.concurrent.Future
import scala.util.control.NonFatal

trait DynamicSuiteExecutor extends CheckSuite {
  def apply(solution: String, suite: String)
           (implicit s: Scheduler): (Observable[Line], Future[String])
}

trait ScalaDynamicRunner extends DynamicSuiteExecutor with DynamicExecution {
  //todo: solutionTrait should be taken from DB and populated during the new task creation
  def findTraitName(suite: String) = traitDefPattern.findFirstIn(suite).get.split( """\s+""")(1)

  /**
   * Runs dynamic solution and dynamic suite
   */
  def apply(solution: String, suite: String)
           (implicit s: Scheduler): (Observable[Line], Future[String]) = {
    val channel = PublishChannel[Line](DropOld(20))
    val f = Future {
      val traitName = FailWithMessage("There is no trait type defined in the Test constructor, code: ") {
        findTraitName(suite)
      }
      val patchedSolution = classDefPattern.replaceFirstIn(solution, s"class $userClass extends $traitName ")
      executeDynamic(channel, suite, patchedSolution)
    }
    f.onFailure {
      case NonFatal(e) => channel.pushNext(Line(e.getMessage))
        channel.pushComplete()
    }
    channel -> f
  }
}

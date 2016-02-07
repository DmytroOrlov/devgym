package service

import monifu.concurrent.Scheduler
import monifu.reactive.Observable
import monifu.reactive.OverflowStrategy.DropOld
import monifu.reactive.channels.PublishChannel
import shared.Line

import scala.concurrent.Future
import scala.util.control.NonFatal

trait ScalaDynamicNoTraitRunner extends DynamicExecution {
  /**
   * Runs dynamic solution as well as dynamic suite using the structural type for test, instead of explicitly defined
   * trait
   */
  def execSuiteNoTrait(solution: String, suite: String)
                      (implicit s: Scheduler): (Observable[Line], Future[String]) = {
    val channel = PublishChannel[Line](DropOld(20))
    val f = Future {
      val patchedSolution = classDefPattern.replaceFirstIn(solution, s"class $userClass ")
      executeDynamic(channel, suite, patchedSolution)
    }
    f.onFailure {
      case NonFatal(e) => channel.pushNext(Line(e.getMessage))
        channel.pushComplete()
    }
    channel -> f
  }
}

package service.reflection

import monifu.concurrent.Scheduler

trait ScalaDynamicNoTraitRunner extends DynamicExecution {
  /**
   * Runs dynamic solution as well as dynamic suite using the structural type for test, instead of explicitly defined
   * trait
   */
  def execSuiteNoTrait(solution: String, suite: String)
                      (channel: String => Unit)
                      (implicit s: Scheduler): String = {
    val patchedSolution = classDefPattern.replaceFirstIn(solution, s"class $userClass ")
    executeDynamic(suite, patchedSolution, channel)
  }
}
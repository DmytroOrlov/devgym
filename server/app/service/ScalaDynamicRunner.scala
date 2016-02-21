package service

import monifu.concurrent.Scheduler

trait DynamicSuiteExecutor {
  def apply(solution: String, suite: String)
           (channel: String => Unit)
           (implicit s: Scheduler): Unit
}

trait ScalaDynamicRunner extends DynamicSuiteExecutor with DynamicExecution {
  //todo: solutionTrait should be taken from DB and populated during the new task creation
  def findTraitName(suite: String) = traitDefPattern.findFirstIn(suite).get.split( """\s+""")(1)

  /**
   * Runs dynamic solution and dynamic suite
   */
  def apply(solution: String, suite: String)
           (channel: String => Unit)
           (implicit s: Scheduler): Unit = {
    val traitName = WithSuiteException(s"There is no solution trait type defined in suite, code: $suite") {
      findTraitName(suite)
    }
    classDefPattern.findFirstIn(solution).orElse(throw new SuiteException(s"There is no class definition in solution code: $solution"))
    val patchedSolution = classDefPattern.replaceFirstIn(solution, s"class $userClass extends $traitName ")
    executeDynamic(suite, patchedSolution, channel)
  }
}

package service.reflection

import monix.execution.Scheduler

trait DynamicSuiteExecutor {
  def apply(solution: String, suite: String, solutionTrait: String)
           (channel: String => Unit)
           (implicit s: Scheduler): String
}

trait ScalaDynamicRunner extends DynamicSuiteExecutor with DynamicExecution {

  /**
    * Runs dynamic solution and dynamic suite
    */
  def apply(solution: String, suite: String, solutionTrait: String)
           (channel: String => Unit)
           (implicit s: Scheduler): String = {

    classDefPattern.findFirstIn(solution)
      //TODO: replace exception message to messageKey
      .orElse(throw SuiteException(s"There is no class definition in solution code: $solution"))
    //TODO: user may already provide extends from solutionTrait, so we may add redundant extends which will validation
    val patchedSolution = classDefPattern.replaceFirstIn(solution, s"class $userClass extends $solutionTrait ")
    executeDynamic(suite, patchedSolution, channel)
  }
}

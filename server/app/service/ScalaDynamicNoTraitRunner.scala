package service

import scala.util.Try

trait ScalaDynamicNoTraitRunner extends DynamicExecution with TryBlock {
  /**
   * Runs dynamic solution as well as dynamic suite using the structural type for test, instead of explicitly defined
   * trait
   */
  def execSuiteNoTrait(solution: String, suite: String): Try[String] = {
    val patchedSolution = classDefPattern.replaceFirstIn(solution, s"class $userClass ")
    executeDynamic(suite, patchedSolution)
  }
}

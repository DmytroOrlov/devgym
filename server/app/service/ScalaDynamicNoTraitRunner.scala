package service

import scala.util.Try

trait ScalaDynamicNoTraitRunner extends ExecuteDynamic with TryBlock {
  /**
   * Runs dynamic solution as well as dynamic suite using the structural type for test, instead of explicitly defined
   * trait
   */
  def execSuiteNoTrait(solution: String, suite: String): Try[String] = for {
    patchedSolution <- tryBlock()(classDefPattern.replaceFirstIn(solution, s"class $userClass "))
    result <- executeDynamic(suite, patchedSolution)
  } yield result
}

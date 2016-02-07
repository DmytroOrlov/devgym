package service

import scala.util.Try

trait DynamicSuiteExecutor {
  def apply(solution: String, suite: String, checked: Boolean): Try[String]
}

trait ScalaDynamicRunner extends DynamicSuiteExecutor with DynamicExecution with TryBlock {
  private val t = new TryBlock {
    override def failurePrefix(s: String): String = s"There is no trait type defined in the Test constructor, code: $s"
  }

  //todo: solutionTrait should be taken from DB and populated during the new task creation
  def findTraitName(suite: String): Try[String] =
    t.tryBlock(suite) {
      traitDefPattern.findFirstIn(suite).get.split( """\s+""")(1)
    }

  /**
   * Runs dynamic solution and dynamic suite
   */
  def apply(solution: String, suite: String, checked: Boolean): Try[String] = for {
    traitName <- findTraitName(suite)
    patchedSolution = classDefPattern.replaceFirstIn(solution, s"class $userClass extends $traitName ")
    r <- executeDynamic(suite, patchedSolution) if !checked || !r.contains(failed)
  } yield r
}

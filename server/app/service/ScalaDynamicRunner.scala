package service

import scala.util.Try

trait ExecDynamicSuite {
  def apply(solution: String, suite: String): Try[String]
}

trait ScalaDynamicRunner extends ExecDynamicSuite with ExecuteDynamic with TryBlock {
  private val tryTrait = new TryBlock {
    override def failurePrefix(s: String): String = s"There is no trait type defined in the Test constructor, code: $s"
  }

  //todo: solutionTrait should be taken from DB and populated during the task creation by user
  def findTraitName(suite: String): Try[String] =
    tryTrait(suite) {
      traitDefPattern.findFirstIn(suite).get.split( """\s+""")(1)
    }

  /**
   * Runs dynamic solution and dynamic suite
   */
  def apply(solution: String, suite: String): Try[String] = for {
    traitName <- findTraitName(suite)
    patchedSolution <- tryBlock()(classDefPattern.replaceFirstIn(solution, s"class $userClass extends $traitName "))
    result <- executeDynamic(suite, patchedSolution)
  } yield result
}

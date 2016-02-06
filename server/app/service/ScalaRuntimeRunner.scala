package service

import org.scalatest.Suite

import scala.util.Try

trait ExecRuntimeSuite {
  def apply(suiteClass: Class[Suite], solutionTrait: Class[AnyRef])(solution: String): Try[String]
}

trait ScalaRuntimeRunner extends ExecRuntimeSuite with ExecuteSuite with TryBlock with SuiteToolbox {
  /**
   * Runs suite loaded in runtime with dynamic solution
   */
  def apply(suiteClass: Class[Suite], solutionTrait: Class[AnyRef])(solution: String): Try[String] =
    tryBlock() {
      def createSolutionInstance(solution: String, solutionTrait: Class[AnyRef]): AnyRef = {
        val patchedSolution = classDefPattern.replaceFirstIn(solution, s"class $userClass extends ${solutionTrait.getSimpleName} ")
        val dynamicCode = s"import ${solutionTrait.getName}; $patchedSolution; new $userClass"

        tb.eval(tb.parse(dynamicCode)).asInstanceOf[AnyRef]
      }

      val solutionInstance = createSolutionInstance(solution, solutionTrait)
      executionOutput(suiteClass.getConstructor(solutionTrait).newInstance(solutionInstance))
    }
}
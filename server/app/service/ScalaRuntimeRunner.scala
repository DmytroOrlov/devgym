package service

import org.scalatest.Suite

import scala.util.Try

trait RuntimeSuiteExecutor {
  def apply(suiteClass: Class[Suite], solutionTrait: Class[AnyRef])
           (checked: Boolean)
           (solution: String): Try[String]
}

trait ScalaRuntimeRunner extends RuntimeSuiteExecutor with SuiteExecution with TryBlock with SuiteToolbox {
  /**
   * Runs suite loaded in runtime with dynamic solution
   */
  def apply(suiteClass: Class[Suite], solutionTrait: Class[AnyRef])
           (checked: Boolean)
           (solution: String): Try[String] = {
    val result = tryBlock() {
      def createSolutionInstance(solution: String, solutionTrait: Class[AnyRef]): AnyRef = {
        val patchedSolution = classDefPattern.replaceFirstIn(solution, s"class $userClass extends ${solutionTrait.getSimpleName} ")
        val dynamicCode = s"import ${solutionTrait.getName}; $patchedSolution; new $userClass"

        tb.eval(tb.parse(dynamicCode)).asInstanceOf[AnyRef]
      }

      val solutionInstance = createSolutionInstance(solution, solutionTrait)
      executionOutput(suiteClass.getConstructor(solutionTrait).newInstance(solutionInstance))
    }
    if (checked) result.filter(!_.contains(failed))
    else result
  }
}

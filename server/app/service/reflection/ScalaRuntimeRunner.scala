package service.reflection

import monix.execution.Scheduler
import org.scalatest.Suite

trait RuntimeSuiteExecutor {
  def apply(suiteClass: Class[Suite], solutionTrait: Class[AnyRef], solution: String)
           (channel: String => Unit)
           (implicit s: Scheduler): Unit
}

trait ScalaRuntimeRunner extends RuntimeSuiteExecutor with SuiteExecution with SuiteToolbox {
  /**
    * Runs suite loaded in runtime with dynamic solution
    */
  def apply(suiteClass: Class[Suite], solutionTrait: Class[AnyRef], solution: String)
           (channel: String => Unit)
           (implicit s: Scheduler): Unit = {
    val solutionInstance = createSolutionInstance(solution, solutionTrait)
    executionTestSuite(suiteClass.getConstructor(solutionTrait).newInstance(solutionInstance), channel)
  }

  def createSolutionInstance(solution: String, solutionTrait: Class[AnyRef]): AnyRef = {
    val patchedSolution = classDefPattern.replaceFirstIn(solution, s"class $userClass extends ${solutionTrait.getSimpleName} ")
    val dynamicCode = s"import ${solutionTrait.getName}; $patchedSolution; new $userClass"

    tb.eval(tb.parse(dynamicCode)).asInstanceOf[AnyRef]
  }
}

package service

import org.scalamock.scalatest.MockFactory
import org.scalatest.{FlatSpec, Matchers, Suite}

import scala.concurrent.ExecutionContext.Implicits.global

abstract class PositiveOutput extends FlatSpec with Matchers with MockFactory {
  def suiteInstance: Suite

  it should "pass all tests for correct solution" in new SuiteExecution with SuiteToolbox {
    StringBuilderRunner(executionOutput(suiteInstance, _)) shouldNot include regex failed
  }
}

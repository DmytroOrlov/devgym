package service.reflection

import org.scalamock.scalatest.MockFactory
import org.scalatest.{FlatSpec, Matchers, Suite}
import service.StringBuilderRunner
import service._

import scala.concurrent.ExecutionContext.Implicits.global

abstract class PositiveOutput extends FlatSpec with Matchers with MockFactory {
  def suiteInstance: Suite

  it should "pass all tests for correct solution" in new SuiteExecution with SuiteToolbox {
    StringBuilderRunner(executionTestSuite(suiteInstance, _)) shouldNot include regex testFailedMarker
  }
}

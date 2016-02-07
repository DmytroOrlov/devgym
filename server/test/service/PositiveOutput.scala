package service

import monifu.reactive.Channel
import org.scalamock.scalatest.MockFactory
import org.scalatest.{FlatSpec, Matchers, Suite}
import shared.Line

abstract class PositiveOutput extends FlatSpec with Matchers with MockFactory {
  def suiteInstance: Suite

  it should "pass all tests for correct solution" in new SuiteExecution with SuiteToolbox {
    executionOutput(stub[Channel[Line]], suiteInstance) shouldNot include regex failed
  }
}

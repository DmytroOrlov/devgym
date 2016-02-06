package service

import org.scalatest.{FlatSpec, Matchers, Suite}

abstract class PositiveOutputTest extends FlatSpec with Matchers {
  def suiteInstance: Suite

  it should "pass all tests for correct solution" in new ExecuteSuite with SuiteToolbox {
    executionOutput(suiteInstance) shouldNot include regex failed
  }
}

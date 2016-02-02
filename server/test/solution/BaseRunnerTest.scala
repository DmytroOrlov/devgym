package solution

import org.scalatest.{FlatSpec, Matchers, Suite}
import service.ScalaTestRunner

abstract class BaseRunnerTest extends FlatSpec with Matchers {
  val suiteInstance: Suite

  it should "pass all tests for correct solution" in {
    ScalaTestRunner.execSuite(suiteInstance) shouldNot include regex ScalaTestRunner.failedMarker
  }
}

package service

import org.scalatest.{FlatSpec, Matchers}
import shared.model.{TestResult, TestStatus}
import shared.view.SuiteReportUtil.red

class TestResultTest extends FlatSpec with Matchers {

  it should "return positive test result when test is completely passed" in {
    //given
    val report = "some report"
    //when
    val (checkNext, getTestResult) = testSync
    val block: (String => Unit) => Unit = (pushNext: String => Unit) => pushNext(report)
    getTestResult(block(checkNext)) match {
      case result: TestResult =>
        //then
        result.status shouldBe TestStatus.Passed.toString
      case _ => fail()
    }
  }

  it should "return negative test result when report has failed cases" in {
    //given
    val report = s"$red some report is *** FAILED ***"
    //when
    val (checkNext, getTestResult) = testSync
    val block: (String => Unit) => Unit = (pushNext: String => Unit) => pushNext(report)
    getTestResult(block(checkNext)) match {
      case result: TestResult =>
        //then
        result.status shouldBe TestStatus.FailedByTest.toString
      case _ => fail()
    }
  }

  it should "return negative test result when solution is not compilable" in {
    //given
    val error = "compilation error"

    def report = throw new RuntimeException(error)

    val (checkNext, getTestResult) = testSync
    val block: (String => Unit) => Unit = (pushNext: String => Unit) => pushNext(report)
    getTestResult(block(checkNext)) match {
      case result: TestResult =>
        //then
        result.errorMessage shouldBe error
        result.status shouldBe TestStatus.FailedByCompilation.toString
      case _ => fail()
    }
  }
}

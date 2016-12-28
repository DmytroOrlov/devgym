package service

import org.scalatest.{FlatSpec, Matchers}
import shared.model.TestStatus

import scala.util.{Failure, Success}

class TestResultTest extends FlatSpec with Matchers {

  it should "return positive test result when test is completely passed" in {
    //given
    val report = Success("some report")
    //when
    val result = testResult(report)
    //then
    result.errorMessage should be (empty)
    result.status shouldBe TestStatus.Passed.toString
  }

  it should "return negative test result when report has failed cases" in {
    //given
    val report = Success(s"some report is ${service.testFailedMarker}")
    //when
    val result = testResult(report)
    //then
    result.errorMessage should be (empty)
    result.status shouldBe TestStatus.FailedByTest.toString
  }

  it should "return negative test result when solution is not compilable" in {
    //given
    val error = "compilation error"
    val report = Failure(new RuntimeException(error))
    //when
    val result = testResult(report)
    //then
    result.errorMessage should be (error)
    result.status shouldBe TestStatus.FailedByCompilation.toString
  }
}

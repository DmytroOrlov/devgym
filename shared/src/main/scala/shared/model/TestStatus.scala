package shared.model

object TestStatus extends Enumeration {
  type TestStatus = Value

  val Passed, FailedByTest, FailedByCompilation = Value
}
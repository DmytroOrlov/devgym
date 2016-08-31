package shared.model

sealed trait Event {
  def name: String

  def timestamp: Long
}

case class Line(value: String, timestamp: Long = System.currentTimeMillis()) extends Event {
  val name = Line.name
}

object Line {
  val name = "line"
}

case class TestResult(status: String, errorMessage: String = "", timestamp: Long = System.currentTimeMillis()) extends Event {
  val name = TestResult.name
  val testStatus = TestStatus.withName(status)
}

object TestResult {
  val name = "testResult"
}

case class Compiling(timestamp: Long = System.currentTimeMillis()) extends Event {
  val name = Compiling.name
}

object Compiling {
  val name = "compiling"
}

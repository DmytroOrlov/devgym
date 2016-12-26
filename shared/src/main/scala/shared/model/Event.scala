package shared.model

sealed trait Event {
  def name: String

  def timestamp: Long
}

case class Line(value: String, timestamp: Long = System.currentTimeMillis()) extends Event {
  val name: String = Line.name
}

object Line {
  val name = "line"
}

case class TestResult(status: String, errorMessage: String = "", timestamp: Long = System.currentTimeMillis()) extends Event {
  val name: String = TestResult.name
  val testStatus = TestStatus.withName(status)
}

object TestResult {
  val name = "testResult"
}

case class Compiling(timestamp: Long = System.currentTimeMillis()) extends Event {
  val name: String = Compiling.name
}

object Compiling {
  val name = "compiling"
}

case class SolutionTemplate(code: String, timestamp: Long = System.currentTimeMillis()) extends Event {
  val name: String = SolutionTemplate.name
}

object SolutionTemplate {
  val name = "solutionTemplate"
}

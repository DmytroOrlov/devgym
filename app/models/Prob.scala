package models

case class Prob(`class`: SolutionType.SolutionType, task: String, blank: String, test: String)

object SolutionType extends Enumeration {
  type SolutionType = Value

  val scalaClass = Value
}

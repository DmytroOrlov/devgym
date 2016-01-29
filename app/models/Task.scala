package models

case class Task(`type`: TaskType.TaskType, taskDescription: String, solutionTemplate: String, referenceSolution: String)

object TaskType extends Enumeration {
  type TaskType = Value

  val scalaClass = Value
}

package models

case class User(name: String, password: String)

case class Task(`type`: TaskType.TaskType, taskDescription: String, solutionTemplate: String, test: String)

object TaskType extends Enumeration {
  type TaskType = Value

  val scalaClass = Value
}

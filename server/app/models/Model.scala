package models

case class User(name: String, password: String)

case class Task(`type`: TaskType.TaskType, name: String, description: String, solutionTemplate: String, referenceSolution: String, suite: String)

object TaskType extends Enumeration {
  type TaskType = Value

  val scalaClass = Value
}

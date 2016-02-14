package models

import java.util.{Date, UUID}


case class User(name: String, password: String)

case class Task(year: Date, `type`: TaskType.TaskType, timeuuid: UUID, name: String, description: String, solutionTemplate: String, referenceSolution: String, suite: String)

case class NewTask(`type`: TaskType.TaskType, name: String, description: String, solutionTemplate: String, referenceSolution: String, suite: String)

object TaskType extends Enumeration {
  type TaskType = Value

  val scalaClass = Value
}

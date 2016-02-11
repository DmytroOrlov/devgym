package models

import java.time.LocalDate
import java.util.UUID


case class User(name: String, password: String)

case class Task(year: LocalDate, `type`: TaskType.TaskType, timeuuid: UUID, name: String, description: String, solutionTemplate: String, referenceSolution: String, suite: String)

object TaskType extends Enumeration {
  type TaskType = Value

  val scalaClass = Value
}

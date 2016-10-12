package models

import java.util.{Date, UUID}

import models.Language.Language


case class User(name: String, password: String) {
  def this(name: String, timeuuid: UUID, password: String) = this(name, password)
}

case class Task(year: Date, lang: Language, timeuuid: UUID, name: String, description: String,
                solutionTemplate: String, referenceSolution: String, suite: String, solutionTrait: String)

case class NewTask(lang: Language, name: String, description: String, solutionTemplate: String,
                   referenceSolution: String, suite: String, solutionTrait: String)

object Language extends Enumeration {
  type Language = Value

  val scalaLang = Value
}

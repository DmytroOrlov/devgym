package models

import java.time.temporal.ChronoUnit
import java.time.{ZoneOffset, ZonedDateTime}
import java.util.{Date, UUID}

import com.datastax.driver.core.utils.UUIDs
import models.Language.Language
import models.Task.yearAsOfJan1


case class User(name: String, password: String, timeuuid: UUID = UUIDs.timeBased())

case class Task(year: Date, lang: Language, timeuuid: UUID, name: String, description: String,
                solutionTemplate: String, referenceSolution: String, suite: String, solutionTrait: String)

object Task {
  val now = 0

  def yearAsOfJan1(ago: Int = now) =
    Date.from(ZonedDateTime.now(ZoneOffset.UTC)
      .truncatedTo(ChronoUnit.DAYS)
      .withDayOfMonth(1).withMonth(1)
      .minusYears(ago).toInstant)
}

case class NewTask(lang: Language, name: String, description: String, solutionTemplate: String,
                   referenceSolution: String, suite: String, solutionTrait: String, year: Date = yearAsOfJan1(),
                   timeuuid: UUID = UUIDs.timeBased())

object Language extends Enumeration {
  type Language = Value

  val scalaLang = Value
}

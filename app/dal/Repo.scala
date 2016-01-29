package dal

import java.time.temporal.ChronoUnit
import java.time.{ZoneOffset, ZonedDateTime}
import java.util.Date

import com.datastax.driver.core.Session
import com.google.inject.Inject
import dal.Repo._
import models.{Task, User}
import util.FutureUtils._

import scala.concurrent.ExecutionContext

class Repo @Inject()(cluster: CassandraCluster)(implicit ec: ExecutionContext) {
  protected lazy val session: Session = cluster.session
  protected lazy val createUserStatement = session.prepare(
    "INSERT INTO user (name, password, timeuuid)" +
      " VALUES (?, ?, NOW()) IF NOT EXISTS")

  protected lazy val addTaskStatement = session.prepare(
    "INSERT INTO task (type, month, timeuuid, task_description, solution_template, reference_solution)" +
      " VALUES (?, ?, NOW(), ?, ?, ?)")

  def create(user: User) = toFuture(session.executeAsync(createUserStatement.bind(user.name, user.password)))

  def addTask(task: Task) = toFutureUnit(
    session.executeAsync(addTaskStatement.bind(task.`type`.toString, month, task.taskDescription, task.solutionTemplate, task.referenceSolution))
  )
}

object Repo {
  def month =
    Date.from(ZonedDateTime.now(ZoneOffset.UTC).truncatedTo(ChronoUnit.DAYS).withDayOfMonth(1).toInstant)
}

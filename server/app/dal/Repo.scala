package dal

import java.time.temporal.ChronoUnit
import java.time.{ZoneOffset, ZonedDateTime}
import java.util.Date

import com.datastax.driver.core.Session
import com.google.inject.Inject
import dal.Repo._
import models.{Task, User}
import util.FutureUtils._
import util.TryFuture

import scala.concurrent.ExecutionContext

class Repo @Inject()(cluster: CassandraCluster)(implicit ec: ExecutionContext) {
  private lazy val session: Session = cluster.session

  private lazy val createUserStatement = session.prepare(
    "INSERT INTO user (name, password, timeuuid)" +
      " VALUES (?, ?, NOW()) IF NOT EXISTS")
  private lazy val addTaskStatement = session.prepare(
    "INSERT INTO task (type, month, timeuuid, task_description, solution_template, reference_solution, test)" +
      " VALUES (?, ?, NOW(), ?, ?, ?, ?)")

  def create(user: User) = TryFuture(toFuture(session.executeAsync(createUserStatement.bind(user.name, user.password))))

  def addTask(task: Task) = TryFuture(toFutureUnit(
    session.executeAsync(addTaskStatement.bind(task.`type`.toString, month, task.taskDescription, task.solutionTemplate, task.referenceSolution, task.test))
  ))
}

object Repo {
  def month =
    Date.from(ZonedDateTime.now(ZoneOffset.UTC).truncatedTo(ChronoUnit.DAYS).withDayOfMonth(1).toInstant)
}

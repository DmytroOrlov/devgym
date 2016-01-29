package dal

import java.time.temporal.ChronoUnit
import java.time.{ZoneOffset, ZonedDateTime}
import java.util.Date

import com.datastax.driver.core.Session
import com.google.inject.Inject
import dal.Repo._
import models.{Prob, User}
import util.FutureUtils._

import scala.concurrent.ExecutionContext

class Repo @Inject()(cluster: CassandraCluster)(implicit ec: ExecutionContext) {
  protected lazy val session: Session = cluster.session
  protected lazy val createUserStatement = session.prepare(
    "INSERT INTO user (name, password, uuid)" +
      " VALUES (?, ?, NOW()) IF NOT EXISTS")

  protected lazy val addClassSolutionStatement = session.prepare(
    "INSERT INTO solution (type, month, uuid, task, blank, test)" +
      " VALUES (?, ?, NOW(), ?, ?, ?)")

  def create(user: User) = toFuture(session.executeAsync(createUserStatement.bind(user.name, user.password)))

  def addSolution(prob: Prob) = toFutureUnit(
    session.executeAsync(addClassSolutionStatement.bind(prob.`class`, month, prob.task, prob.blank, prob.test))
  )
}

object Repo {
  def month =
    Date.from(ZonedDateTime.now(ZoneOffset.UTC).truncatedTo(ChronoUnit.DAYS).withDayOfMonth(1).toInstant)
}

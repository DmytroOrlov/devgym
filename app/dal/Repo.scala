package dal

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
    "INSERT INTO solution (type, week, task, blank, test)" +
      " VALUES (?, ?, ?, ?, ?)")

  def create(user: User) = toFuture(session.executeAsync(createUserStatement.bind(user.name, user.password)))

  def addSolution(prob: Prob) = {
    val week = new Date({
      val nowMillis = System.currentTimeMillis()
      nowMillis - nowMillis % oneWeekMillis
    })
    toFutureUnit(session.executeAsync(addClassSolutionStatement.bind(prob.`class`, week, prob.task, prob.blank, prob.test)))
  }
}

object Repo {
  val oneWeekMillis = 7 * 24 * 60 * 60 * 1000
}

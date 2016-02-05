package dal

import java.time.temporal.ChronoUnit
import java.time.{ZoneOffset, ZonedDateTime}
import java.util.Date

import com.datastax.driver.core.{ResultSet, Row, Session}
import com.google.inject.Inject
import dal.Dao._
import models.TaskType._
import models.{Task, User}
import util.FutureUtils._
import util.TryFuture

import scala.concurrent.{ExecutionContext, Future}

trait Dao {
  def create(user: User): Future[Boolean]

  def addTask(task: Task): Future[Unit]

  def getTasks(`type`: TaskType, limit: Int, yearAgo: Int): Future[Iterable[Task]]
}

class DaoImpl @Inject()(cluster: CassandraCluster)(implicit ec: ExecutionContext) extends Dao {
  private lazy val session: Session = cluster.session

  private lazy val createUserStatement = session.prepare(
    "INSERT INTO user (name, password, timeuuid)" +
      " VALUES (?, ?, NOW()) IF NOT EXISTS")
  private lazy val addTaskStatement = session.prepare(
    "INSERT INTO task (year, type, timeuuid, task_description, solution_template, reference_solution, suite)" +
      " VALUES (?, ?, NOW(), ?, ?, ?, ?)")
  private lazy val getLastTasksStatement = session.prepare("SELECT year, type, timeuuid, task_description, solution_template, reference_solution, suite FROM task WHERE" +
    " year = ?" +
    " and type = ?" +
    " limit ?")

  private def toTask(r: Row) = Task(
    models.TaskType.withName(r.getString("type")),
    r.getString("task_description"),
    r.getString("solution_template"),
    r.getString("reference_solution"),
    r.getString("suite")
  )

  private def all[T](f: Row => T)(res: ResultSet): Iterable[T] = {
    import scala.collection.JavaConversions._
    res.map(f)
  }

  private def allTasks = all(toTask) _

  def create(user: User): Future[Boolean] = TryFuture(toFuture(session.executeAsync(createUserStatement.bind(user.name, user.password)))).map(_.one().getBool(applied))

  def addTask(task: Task): Future[Unit] = TryFuture(toFutureUnit {
    session.executeAsync(addTaskStatement.bind(year(), task.`type`.toString, task.taskDescription, task.solutionTemplate, task.referenceSolution, task.suite))
  })

  def getTasks(`type`: TaskType, limit: Int, yearAgo: Int): Future[Iterable[Task]] = TryFuture(
    toFuture {
      val limitInt: Integer = limit
      session.executeAsync(getLastTasksStatement.bind(year(yearAgo), `type`.toString, limitInt))
    }.map(allTasks))
}

object Dao {
  val applied = "[applied]"

  val now = 0

  def year(ago: Int = now) =
    Date.from(ZonedDateTime.now(ZoneOffset.UTC)
      .truncatedTo(ChronoUnit.DAYS)
      .withDayOfMonth(1).withMonth(1)
      .minusYears(ago).toInstant)
}

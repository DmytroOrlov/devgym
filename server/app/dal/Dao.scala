package dal

import java.time.temporal.ChronoUnit
import java.time.{ZoneOffset, ZonedDateTime}
import java.util.{Date, UUID}

import com.datastax.driver.core.{ResultSet, Row, Session}
import com.google.inject.Inject
import dal.Dao._
import models.TaskType._
import models.{NewTask, Task, TaskType, User}
import util.FutureUtils._
import util.TryFuture

import scala.concurrent.{ExecutionContext, Future}

trait Dao {
  def create(user: User): Future[Boolean]

  def addTask(task: NewTask): Future[Unit]

  def getTasks(`type`: TaskType, limit: Int, yearAgo: Int): Future[Iterable[Task]]

  def getTask(year: Date, taskType: TaskType.TaskType, timeuuid: UUID): Future[Option[Task]]
}

class DaoImpl @Inject()(cluster: CassandraCluster)(implicit ec: ExecutionContext) extends Dao {
  private lazy val session: Session = cluster.session

  private lazy val createUserStatement = session.prepare(
    "INSERT INTO user (name, password, timeuuid)" +
      " VALUES (?, ?, NOW()) IF NOT EXISTS")
  private lazy val addTaskStatement = session.prepare(
    "INSERT INTO task (year, type, timeuuid, name, description, solution_template, reference_solution, suite, solution_trait)" +
      " VALUES (?, ?, NOW(), ?, ?, ?, ?, ?)")
  private lazy val getLastTasksStatement = session.prepare("SELECT year, type, timeuuid, name, description, solution_template, reference_solution, suite, solution_trait FROM task WHERE" +
    " year = ?" +
    " and type = ?" +
    " limit ?")
  private lazy val getTaskStatement = session.prepare("SELECT * FROM task WHERE " +
    " year = ?" +
    " and type = ?" +
    " and timeuuid = ?")

  private def toTask(r: Row) = Task(
    r.getTimestamp("year"),
    models.TaskType.withName(r.getString("type")),
    r.getUUID("timeuuid"),
    r.getString("name"),
    r.getString("description"),
    r.getString("solution_template"),
    r.getString("reference_solution"),
    r.getString("suite"),
    r.getString("solution_trait")
  )

  private def all[T](f: Row => T)(res: ResultSet): Iterable[T] = {
    import scala.collection.JavaConversions._
    res.map(f)
  }

  private def one[T](f: Row => T)(res: ResultSet): Option[T] = {
    if (res.isExhausted) None
    else Some(f(res.one()))
  }

  private def allTasks = all(toTask) _
  private def oneTask = one(toTask) _

  def create(user: User): Future[Boolean] = TryFuture(toFuture(
    session.executeAsync(createUserStatement.bind(user.name, user.password)))).map(_.one().getBool(applied))

  def addTask(task: NewTask): Future[Unit] = TryFuture(toFutureUnit {
    session.executeAsync(addTaskStatement.bind(yearAsOfJan1(), task.`type`.toString, task.name, task.description,
      task.solutionTemplate, task.referenceSolution, task.suite))
  })

  def getTasks(`type`: TaskType, limit: Int, yearAgo: Int): Future[Iterable[Task]] = TryFuture(
    toFuture {
      val limitInt: Integer = limit
      session.executeAsync(getLastTasksStatement.bind(yearAsOfJan1(yearAgo), `type`.toString, limitInt))
    }.map(allTasks))

  def getTask(year: Date, `type`: TaskType, timeuuid: UUID): Future[Option[Task]] = TryFuture(
    toFuture {
      session.executeAsync(getTaskStatement.bind(year, `type`.toString, timeuuid))
    }.map(oneTask))
}

object Dao {
  val applied = "[applied]"

  val now = 0

  def yearAsOfJan1(ago: Int = now) =
    Date.from(ZonedDateTime.now(ZoneOffset.UTC)
      .truncatedTo(ChronoUnit.DAYS)
      .withDayOfMonth(1).withMonth(1)
      .minusYears(ago).toInstant)
}

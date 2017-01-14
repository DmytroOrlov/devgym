package data

import java.util.{Date, UUID}
import javax.inject.Singleton

import com.google.inject.{ImplementedBy, Inject}
import io.getquill.MappedEncoding
import models.Language._
import models.Task.yearAsOfJan1
import models.{Language, NewTask, Task}

import scala.concurrent.{ExecutionContext, Future}
import scala.util.control.NonFatal

@ImplementedBy(classOf[TaskDaoImpl])
trait TaskDao {
  def addTask(task: NewTask): Future[Unit]

  def getTasks(lang: Language, limit: Int, yearAgo: Int): Future[Iterable[Task]]

  def getTask(year: Date, lang: Language.Language, timeuuid: UUID): Future[Option[Task]]
}

@Singleton
class TaskDaoImpl @Inject()(val ctx: CassandraAsyncContextImpl)
                           (implicit ec: ExecutionContext) extends TaskDao {
  implicit val encodeUUID = MappedEncoding[Language, String](_.toString)
  implicit val decodeUUID = MappedEncoding[String, Language](Language.withName)
  val db = ctx

  import db._

  val lastTask = (year: Date, lang: Language, limit: Int) => quote {
    query[Task]
      .filter(t => t.year == lift(year))
      .filter(t => t.lang == lift(lang))
      .take(lift(limit))
  }

  val task = (year: Date, lang: Language, timeuuid: UUID) => quote {
    query[Task]
      .filter(t => t.year == lift(year))
      .filter(t => t.lang == lift(lang))
      .filter(t => t.timeuuid == lift(timeuuid))
  }

  val insertTask = (task: NewTask) => quote {
    querySchema[NewTask]("task").insert(lift(task))
  }

  def getTasks(lang: Language, limit: Int, yearAgo: Int): Future[List[Task]] = {
    db.run(lastTask(yearAsOfJan1(yearAgo), lang, limit))
  }

  def getTask(year: Date, lang: Language, timeuuid: UUID): Future[Option[Task]] =
    db.run(task(year, lang, timeuuid)).collect {
      case x :: xs => Some(x)
      case Nil => None
    } recover {
      case NonFatal(e) => None
    }

  def addTask(task: NewTask): Future[Unit] = db.run(insertTask(task))
}

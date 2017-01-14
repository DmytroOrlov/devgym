package data

import javax.inject.Singleton

import com.google.inject.{ImplementedBy, Inject}
import io.getquill.{CassandraAsyncContext, SnakeCase}
import models.User

import scala.concurrent.{ExecutionContext, Future}
import scala.util.control.NonFatal

@ImplementedBy(classOf[UserDaoImpl])
trait UserDao {
  def create(user: User): Future[Boolean]

  def find(userName: String): Future[Option[User]]
}

@Singleton
class UserDaoImpl @Inject()(val ctx: CassandraAsyncContextImpl)(implicit ec: ExecutionContext) extends UserDao {
  val db = ctx

  import db._

  val user = (userName: String) => quote {
    query[User]
      .filter(u => u.name == lift(userName))
  }

  def find(userName: String): Future[Option[User]] = {
    db.run(user(userName)).collect {
      case x :: xs => Some(x)
      case Nil => None
    } recover {
      case NonFatal(e) => None
    }
  }

  val insert = (user: User) => quote {
    query[User].insert(lift(user))
  }

  def create(user: User): Future[Boolean] = {
    db.run(insert(user)).map(_ => true)
  }
}

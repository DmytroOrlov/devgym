package dal

import com.datastax.driver.core.Session
import com.google.inject.Inject
import models.User
import util.FutureUtils._

import scala.concurrent.{ExecutionContext, Future}

class Repo @Inject()(cluster: CassandraCluster)(implicit ec: ExecutionContext) {
  protected lazy val session: Session = cluster.session
  protected lazy val createUserStatement = session.prepare(
    "INSERT INTO user (name, password, uuid)" +
      " VALUES (?, ?, NOW()) IF NOT EXISTS")

  def create(user: User): Future[Unit] = toFutureUnit(session.executeAsync(createUserStatement.bind(user.name, user.password)))
}

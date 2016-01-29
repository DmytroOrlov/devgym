package dal

import com.datastax.driver.core.{Cluster, Session}
import com.google.inject.{Inject, Singleton}
import play.api.Configuration
import play.api.inject.ApplicationLifecycle
import util.FutureUtils.toFutureUnit

import scala.concurrent.ExecutionContext

@Singleton
class CassandraCluster @Inject()(conf: CassandraConfig, appLifecycle: ApplicationLifecycle)(implicit executor: ExecutionContext) {
  private val cluster = {
    Cluster.builder()
      .addContactPoints(conf.hosts: _*)
      .withPort(conf.port)
      .build()
  }

  def session: Session = cluster.connect(conf.keySpace)

  private def stop() = toFutureUnit(cluster.closeAsync())

  appLifecycle.addStopHook(() => stop())
}

@Singleton
class CassandraConfig @Inject()(config: Configuration) {
  val keySpace = config.getString("devgym.db.cassandra.keyspace").get
  val port = config.getInt("devgym.db.cassandra.port").get
  val hosts: Seq[String] = {
    import scala.collection.JavaConversions._
    config.getStringList("devgym.db.cassandra.hosts").get
  }
}

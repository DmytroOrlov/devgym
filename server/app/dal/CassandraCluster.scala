package dal

import com.datastax.driver.core.{Cluster, Session}
import com.google.inject.{Inject, Singleton}
import com.typesafe.config.Config
import play.api.Configuration
import play.api.inject.ApplicationLifecycle
import util.FutureUtils.toFutureUnit

import scala.concurrent.ExecutionContext
import scala.sys.process._
import scala.util.Try

@Singleton
class CassandraCluster @Inject()(conf: CassandraConfig, appLifecycle: ApplicationLifecycle)(implicit executor: ExecutionContext) {
  private lazy val cluster =
    Cluster.builder()
      .addContactPoints(conf.hosts: _*)
      .withPort(conf.port)
      .build()

  def session: Session = cluster.connect(conf.keySpace)

  private def stop() = toFutureUnit(cluster.closeAsync())

  appLifecycle.addStopHook(() => stop())
}

@Singleton
class CassandraConfig @Inject()(configuration: Configuration) {
  val config: Config = configuration.underlying

  val keySpace = config.getString("devgym.db.cassandra.keyspace")
  val port = config.getInt("devgym.db.cassandra.port")

  lazy val hosts: Seq[String] = {
    def hosts: Seq[String] = {
      import scala.collection.JavaConversions._
      config.getStringList("devgym.db.cassandra.hosts")
    }

    def docker: Option[Boolean] = configuration.getBoolean("devgym.db.cassandra.docker")

    def ip() = docker match {
      case Some(true) => Try(Seq("docker-machine", "ip", "default").!!.trim).toOption
      case _ => None
    }

    ip().fold(hosts) { ip => Seq(ip) }
  }
}

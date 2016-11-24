package dal

import com.datastax.driver.core.{Cluster, Session}
import com.google.inject.{Inject, Singleton}
import com.typesafe.config.Config
import play.api.inject.ApplicationLifecycle
import play.api.{Configuration, Environment, Logger}
import util.FutureUtils.toFutureUnit

import scala.concurrent.ExecutionContext

@Singleton
class CassandraCluster @Inject()(conf: CassandraConfig, appLifecycle: ApplicationLifecycle)(implicit executor: ExecutionContext) {
  private val hosts = conf.hosts
  private lazy val cluster =
    Cluster.builder()
      .addContactPoints(hosts: _*)
      .withPort(conf.port)
      .build()

  def noKeySpaceSession: Session = cluster.connect()

  def session: Session = cluster.connect(conf.keySpace)

  def stop() = toFutureUnit(cluster.closeAsync())

  Logger.info(s"Cassandra host to be used : '${hosts.mkString(",")}' with port:${conf.port}")
  appLifecycle.addStopHook(() => stop())
}

@Singleton
class CassandraConfig @Inject()(configuration: Configuration, environment: Environment) {
  val config: Config = configuration.underlying

  val keySpace = config.getString("devgym.db.cassandra.keyspace")
  val port = config.getInt("devgym.db.cassandra.port")

  val hosts: Seq[String] =
    configuration.getStringSeq("devgym.db.cassandra.hosts").get
}

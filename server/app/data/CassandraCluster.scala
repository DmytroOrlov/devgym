package data

import javax.inject.{Inject, Singleton}

import com.datastax.driver.core.{Cluster, Session}
import com.typesafe.config.Config
import io.getquill.{CassandraAsyncContext, SnakeCase}
import play.api.inject.ApplicationLifecycle
import play.api.{Configuration, Environment, Logger}
import util.FutureUtils.toFutureUnit

import scala.concurrent.ExecutionContext

@Singleton
class CassandraAsyncContextImpl @Inject()(cassandra: CassandraCluster, conf: CassandraConfig)
  extends CassandraAsyncContext[SnakeCase](cassandra.cluster, conf.keySpace, 100L)

@Singleton
class CassandraCluster @Inject()(conf: CassandraConfig, appLifecycle: ApplicationLifecycle)(implicit executor: ExecutionContext) {

  import conf._

  private[data] val cluster =
    Cluster.builder()
      .addContactPoints(hosts: _*)
      .withPort(port)
      .build()

  private[data] def noKeySpaceSession: Session = cluster.connect()

  private[data] def stop() = toFutureUnit(cluster.closeAsync())

  Logger.info(s"Cassandra host to be used: '${hosts.mkString(",")}' with port:$port")
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

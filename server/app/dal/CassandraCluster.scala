package dal

import com.datastax.driver.core.{Cluster, Session}
import com.google.inject.{Inject, Singleton}
import com.typesafe.config.Config
import play.api.inject.ApplicationLifecycle
import play.api.{Configuration, Environment, Logger, Mode}
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

  def noKeySpaceSession: Session = cluster.connect()
  def session: Session = cluster.connect(conf.keySpace)

  def stop() = toFutureUnit(cluster.closeAsync())

  appLifecycle.addStopHook(() => stop())
}

@Singleton
class CassandraConfig @Inject()(configuration: Configuration, environment: Environment) {
  val config: Config = configuration.underlying

  val keySpace = config.getString("devgym.db.cassandra.keyspace")
  val port = config.getInt("devgym.db.cassandra.port")

  lazy val hosts: Seq[String] = {
    val hostsConf = "devgym.db.cassandra.hosts"
    val dockerConf = "devgym.db.cassandra.docker"

    def hosts: Seq[String] = {
      import scala.collection.JavaConversions._
      config.getStringList(hostsConf)
    }

    def docker: Option[Boolean] = configuration.getBoolean(dockerConf)

    def dockerIp() = docker match {
      case Some(true) =>
        if (environment.mode == Mode.Prod)
          Logger.error(s"$dockerConf = true, and we are in prod mode")
        Try(Seq("docker-machine", "ip", "default").!!.trim).toOption
      case _ => None
    }

    dockerIp().fold(hosts) { ip =>
      Logger.warn(s"$hostsConf overridden with docker $ip")
      Seq(ip)
    }
  }
}

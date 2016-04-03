package dal

import com.datastax.driver.core.{Cluster, Session}
import com.google.inject.{Inject, Singleton}
import com.typesafe.config.Config
import play.api.inject.ApplicationLifecycle
import play.api.{Configuration, Environment}
import util.FutureUtils.toFutureUnit
import play.api.Logger

import scala.concurrent.ExecutionContext
import scala.sys.process._
import scala.util.{Failure, Success, Try}
import scala.util.matching.Regex

@Singleton
class CassandraCluster @Inject()(conf: CassandraConfig, appLifecycle: ApplicationLifecycle)(implicit executor: ExecutionContext) {
  private lazy val hosts = conf.hosts
  private lazy val cluster =
    Cluster.builder()
      .addContactPoints(hosts: _*)
      .withPort(conf.port)
      .build()

  Logger.info(s"Cassandra host to be used: ${hosts.mkString(",")}:${conf.port}")

  def noKeySpaceSession: Session = cluster.connect()
  def session: Session = cluster.connect(conf.keySpace)

  def stop() = toFutureUnit(cluster.closeAsync())

  appLifecycle.addStopHook(() => stop())
}

@Singleton
class CassandraConfig @Inject()(configuration: Configuration, environment: Environment) {
  val config: Config = configuration.underlying

  val cassandraHostname = "cassandra"
  val keySpace = config.getString("devgym.db.cassandra.keyspace")
  val port = config.getInt("devgym.db.cassandra.port")

  lazy val hosts: Seq[String] = {
    val hostsConf = "devgym.db.cassandra.hosts"
    val dockerConf = "devgym.db.cassandra.docker"

    def hosts: Seq[String] = {
      import scala.collection.JavaConversions._
      config.getStringList(hostsConf)
    }

    val r = new Regex( """(\$\()([^)]*)(\))""", "$(", "command", ")")
    hosts.map { h =>
      r.replaceAllIn(h, m => {
        val ip = Try(m.group("command").!!)
        ip match {
          case Success(s) => s.trim
          case Failure(_) => cassandraHostname
        }
      })
    }
  }
}

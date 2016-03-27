package data

import java.io.File
import java.nio.file.Paths

import com.datastax.driver.core.Session
import dal.{CassandraCluster, CassandraConfig}
import monifu.concurrent.Implicits.globalScheduler
import play.api.inject.ApplicationLifecycle
import play.api.{Configuration, Environment}

import scala.concurrent.Future
import scala.util.{Failure, Success, Try}

object DataLoader extends App {
  val blockSeparator = "\n/"
  val scriptsPath = "cassandra"
  val configPath = "server/conf/application.conf"

  val env = Environment.simple(new File(configPath))
  val cassandraConfig = new CassandraConfig(Configuration.load(env), env)

  getCluster match {
    case Success(cluster) =>
      val session = cluster.noKeySpaceSession
      try {
        dropKeySpace(cassandraConfig.keySpace, session)
        println("CQL scripts import start...")
        executeScripts(block => session.execute(block))
        println("CQL scripts import completed")
      } finally {
        session.close()
        cluster.stop()
      }
    case Failure(e) => print(s"cassandra instance error: ${e.getMessage}")
  }

  private def getCluster = Try(
    new CassandraCluster(cassandraConfig, new ApplicationLifecycle {
      override def addStopHook(hook: () => Future[_]): Unit = hook
    })
  )

  private def dropKeySpace(keySpace: String, session: Session) =
    Try(session.execute(s"drop schema $keySpace")) match {
      case Success(AnyRef) => println("key space has been dropped")
      case Failure(e) => println(s"drop of key space has been failed, error: ${e.getMessage}")
    }

  private def executeScripts(executor: String => Any) = {
    Paths.get(scriptsPath).toFile.listFiles().foreach { f =>
      println(s"source file: ${f.getAbsolutePath}")

      val source = scala.io.Source.fromFile(f.getAbsolutePath)
      val blocks = try {
        source.mkString.split(blockSeparator)
      } finally source.close()

      blocks.foreach { b =>
        println(s"Running CQL:\n $b")
        executor(b)
      }
    }
  }
}

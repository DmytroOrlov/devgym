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

  val cassandraConfig = new CassandraConfig(Configuration.load(Environment.simple(new File(configPath))))

  getCluster match {
    case Success(cluster) =>
      val session = cluster.noKeySpaceSession
      try {
        println("CQL scripts import start...")
        dropKeySpace(cassandraConfig.keySpace, session)
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

  private def dropKeySpace(keySpace: String, session: Session) = session.execute(s"drop schema $keySpace")

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

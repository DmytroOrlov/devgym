package data

import com.datastax.driver.core.Session
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.{Logger, Play}

import scala.concurrent.Await
import scala.concurrent.duration._
import scala.util.{Failure, Success, Try}

object DataLoader extends App {
  val blockSeparator = "\n/"
  val scriptsPath = "cassandra"

  lazy val app = new GuiceApplicationBuilder().build()

  lazy val cassandraConfig = app.injector.instanceOf[CassandraConfig]

  Try {
    val cluster = app.injector.instanceOf[CassandraCluster]
    val session = cluster.noKeySpaceSession
    Logger.info("CQL scripts import start...")
    if (dropOptionEnabled(args)) dropKeySpace(cassandraConfig.keySpace, session)
    executeScripts(block => session.execute(block))
    Logger.info("CQL scripts import completed")
    Await.ready(cluster.stop(), Duration.Inf)
  } match {
    case Success(_) =>
      Play.stop(app)
      System.exit(0)

    case Failure(ex) =>
      val msg = "an error occurred during evolutions run"
      Logger.error(msg, ex)
      System.err.println(msg)
      ex.printStackTrace(System.err)
      System.exit(1)
  }

  private def dropOptionEnabled(args: Array[String]) = args.headOption.contains("drop")

  private def dropKeySpace(keySpace: String, session: Session) =
    Try(session.execute(s"drop schema $keySpace")) match {
      case Success(_) => Logger.info("key space has been dropped")
      case Failure(e) => Logger.warn(s"drop of key space has been failed, error: ${e.getMessage}")
    }

  private def executeScripts(executor: String => Any) = {
    app.path.listFiles().filter(_.getName == scriptsPath).foreach(_.listFiles().sorted.foreach { f =>
      Logger.info(s"source file: ${f.getAbsolutePath}")

      val source = scala.io.Source.fromFile(f.getAbsolutePath)
      val blocks = try {
        source.mkString.split(blockSeparator).map(_.trim).filterNot(_.isEmpty)
      } finally source.close()

      blocks.foreach { b =>
        Logger.info(s"Running CQL:\n $b")
        executor(b)
      }
    })
  }
}

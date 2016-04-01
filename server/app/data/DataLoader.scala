package data

import dal.{CassandraCluster, CassandraConfig}
import monifu.concurrent.Implicits.globalScheduler
import play.api.Play
import play.api.inject.ApplicationLifecycle
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.Logger

import scala.concurrent.Future
import scala.util.{Failure, Success, Try}

object DataLoader extends App {
  val blockSeparator = "\n/"
  val scriptsPath = "cassandra"

  lazy val app = new GuiceApplicationBuilder().build()

  lazy val cassandraConfig = app.injector.instanceOf[CassandraConfig]

  getCluster match {
    case Success(cluster) =>
      val session = cluster.noKeySpaceSession
      try {
        Logger.info("CQL scripts import start...")
        executeScripts(block => session.execute(block))
        Logger.info("CQL scripts import completed")
      } finally {
        session.close()
        cluster.stop()
        Play.stop(app)
      }
    case Failure(e) => Logger.error(s"cassandra instance error: ${e.getMessage}")
  }

  private def getCluster = Try(
    new CassandraCluster(cassandraConfig, new ApplicationLifecycle {
      override def addStopHook(hook: () => Future[_]): Unit = ()
    })
  )

  private def executeScripts(executor: String => Any) = {
    app.path.listFiles().filter(_.getName == scriptsPath).foreach(_.listFiles().foreach { f =>
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

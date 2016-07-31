package controllers

import scala.sys.process._
import scala.util.Try

// keeping for future SbtTestRunner
object SbtTaskSolver {

  def sbt(command: String): Try[Boolean] = Try(Seq("sbt", command).! == 0)

  // no exception, so sbt is in the PATH
  lazy val sbtInstalled = sbt("--version").isSuccess
}

package controllers

import org.scalatest._
import org.scalatestplus.play._
import play.api.inject.guice._

class OneAppSpecs extends Suites(
  new ApplicationTest,
  new TaskSolverTest
) with OneAppPerSuite {
  implicit override lazy val app = new GuiceApplicationBuilder()
    .configure(Map(
      "ehcacheplugin" -> "disabled",
      "devgym.db.cassandra.hosts" -> Seq("cassandra")))
    .build()
}

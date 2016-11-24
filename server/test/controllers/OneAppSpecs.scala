package controllers

import org.scalatest._
import org.scalatestplus.play._
import play.api.inject.guice._

class OneAppSpecs extends Suites(
  new ApplicationTest,
  new TaskSolverTest,
  new AddTaskTest,
  new UserControllerTest,
  new GitHubUserTest
) with OneAppPerSuite {
  implicit override lazy val app = new GuiceApplicationBuilder()
    .configure(Map("ehcacheplugin" -> "disabled"))
    .build()
}

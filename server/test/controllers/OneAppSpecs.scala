package controllers

import data.CassandraAsyncContextImpl
import org.scalatest._
import org.scalatestplus.play._
import play.api.inject.bind
import play.api.inject.guice._

class OneAppSpecs extends Suites(
  new ApplicationTest,
  new TaskSolverTest,
  new AddTaskTest,
  new UserControllerTest,
  new GitHubUserTest
) with OneAppPerSuite with MockitoSugar {

  implicit override lazy val app = new GuiceApplicationBuilder()
    .configure(Map("ehcacheplugin" -> "disabled"))
    .overrides(bind[CassandraAsyncContextImpl] to mockito[CassandraAsyncContextImpl])
    .build()
}

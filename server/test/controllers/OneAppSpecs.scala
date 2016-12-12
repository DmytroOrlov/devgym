package controllers

import com.google.inject.AbstractModule
import com.google.inject.name.Names
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
    .bindings(
      new AbstractModule {
        override def configure(): Unit =
          bind(classOf[String]).annotatedWith(Names named "Secret").toInstance("devgym")
      }
    )
    .build()
}

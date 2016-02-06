package config

import com.google.inject.{AbstractModule, Provides, Singleton}
import com.typesafe.config.Config
import dal.{Dao, DaoImpl}
import play.api.Configuration
import service.{ScalaTestRunner, ScalaTestRunnerContract}

class DevgymModule extends AbstractModule {
  override def configure() = {
    bind(classOf[Dao]) to classOf[DaoImpl]
  }

  @Provides
  def scalaTestRunner(): ScalaTestRunnerContract = ScalaTestRunner

  @Provides @Singleton
  def config(c: Configuration): Config = c.underlying
}

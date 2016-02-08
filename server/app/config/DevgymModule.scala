package config

import com.google.inject.{AbstractModule, Provides, Singleton}
import com.typesafe.config.Config
import dal.{Dao, DaoImpl}
import monifu.concurrent.Scheduler
import play.api.Configuration
import service.{DynamicSuiteExecutor, RuntimeSuiteExecutor, ScalaTestRunner}

import scala.concurrent.ExecutionContext

class DevgymModule extends AbstractModule {
  override def configure() = {
    bind(classOf[Dao]) to classOf[DaoImpl]
    bind(classOf[RuntimeSuiteExecutor]) to classOf[ScalaTestRunner]
    bind(classOf[DynamicSuiteExecutor]) to classOf[ScalaTestRunner]
  }

  @Provides @Singleton
  def config(ec: ExecutionContext): Scheduler = Scheduler(ec)

  @Provides @Singleton
  def config(c: Configuration): Config = c.underlying
}

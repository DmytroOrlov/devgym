package config

import javax.inject.{Named, Singleton}

import com.google.inject.{AbstractModule, Provides}
import com.typesafe.config.Config
import dal.{Dao, DaoImpl}
import monifu.concurrent.Scheduler
import play.api.Configuration
import service.reflection.{DynamicSuiteExecutor, RuntimeSuiteExecutor, ScalaTestRunner}

import scala.concurrent.ExecutionContext
import scala.util.Random

class DevgymModule extends AbstractModule {
  override def configure() = {
    bind(classOf[Dao]) to classOf[DaoImpl]
    bind(classOf[RuntimeSuiteExecutor]) to classOf[ScalaTestRunner]
    bind(classOf[DynamicSuiteExecutor]) to classOf[ScalaTestRunner]
  }

  @Provides
  @Singleton
  def config(ec: ExecutionContext): Scheduler = Scheduler(ec)

  @Provides
  @Singleton
  def config(c: Configuration): Config = c.underlying

  @Provides
  @Singleton
  @Named("Secret")
  def config(): String = "devgym_" + Random.nextInt(9999999)
}

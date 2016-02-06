package config

import com.google.inject.{AbstractModule, Provides, Singleton}
import com.typesafe.config.Config
import dal.{Dao, DaoImpl}
import play.api.Configuration
import service.{ExecDynamicSuite, ExecRuntimeSuite, ScalaTestRunner}

class DevgymModule extends AbstractModule {
  override def configure() = {
    bind(classOf[Dao]) to classOf[DaoImpl]
    bind(classOf[ExecDynamicSuite]) to classOf[ScalaTestRunner]
    bind(classOf[ExecRuntimeSuite]) to classOf[ScalaTestRunner]
  }

  @Provides @Singleton
  def config(c: Configuration): Config = c.underlying
}

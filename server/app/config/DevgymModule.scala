package config

import com.google.inject.{AbstractModule, Provides, Singleton}
import com.typesafe.config.Config
import play.api.Configuration

class DevgymModule extends AbstractModule {
  override def configure(): Unit = {}

  @Provides @Singleton
  def config(c: Configuration): Config = c.underlying
}

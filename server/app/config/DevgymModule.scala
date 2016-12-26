package config

import javax.inject.{Named, Singleton}

import com.google.inject.{AbstractModule, Provides}
import com.typesafe.config.Config
import dal._
import io.getquill.{CassandraAsyncContext, SnakeCase}
import monifu.concurrent.Scheduler
import play.api.Configuration
import service.reflection.{DynamicSuiteExecutor, RuntimeSuiteExecutor, ScalaTestRunner}

import scala.concurrent.ExecutionContext
import scala.util.Random

class DevgymModule extends AbstractModule {
  override def configure() = {
    bind(classOf[TaskDao]) to classOf[TaskDaoImpl]
    bind(classOf[UserDao]) to classOf[UserDaoImpl]
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
  def config(cassandra: CassandraCluster): () => CassandraAsyncContext[SnakeCase] =
    () => new CassandraAsyncContext[SnakeCase](cassandra.cluster, cassandra.keySpace, 100L)

  @Provides
  @Named("Secret")
  def config(): String = "devgym_" + Random.nextInt(9999999)
}

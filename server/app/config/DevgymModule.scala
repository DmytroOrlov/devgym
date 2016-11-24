package config

import com.google.inject.{AbstractModule, Provides, Singleton}
import com.typesafe.config.Config
import dal._
import io.getquill.{CassandraAsyncContext, SnakeCase}
import monifu.concurrent.Scheduler
import play.api.Configuration
import service.reflection.{DynamicSuiteExecutor, RuntimeSuiteExecutor, ScalaTestRunner}

import scala.concurrent.ExecutionContext

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
  def config(cluster: CassandraCluster): CassandraAsyncContext[SnakeCase] =
    new CassandraAsyncContext[SnakeCase](cluster.cluster, cluster.keySpace, 100L)
}

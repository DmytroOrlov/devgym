package controllers

import java.util.concurrent.TimeUnit
import java.util.{Date, UUID}

import akka.actor.ActorSystem
import akka.stream.Materializer
import com.google.inject.Inject
import controllers.TaskSolver._
import controllers.UserController._
import dal.Dao
import models.{Task, TaskType}
import monifu.concurrent.Scheduler
import monifu.reactive.Observable
import org.scalatest.Suite
import play.api.Logger
import play.api.cache.CacheApi
import play.api.data.Form
import play.api.data.Forms._
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.libs.json.JsValue
import play.api.libs.streams.ActorFlow
import play.api.mvc.{Action, Controller, WebSocket}
import service._
import service.reflection.{DynamicSuiteExecutor, RuntimeSuiteExecutor}
import shared.model.{Compiling, Line}
import util.TryFuture

import scala.concurrent.Future
import scala.concurrent.duration._
import scala.util.control.NonFatal

class TaskSolver @Inject()(executor: RuntimeSuiteExecutor with DynamicSuiteExecutor, dao: Dao, val messagesApi: MessagesApi, cache: CacheApi)
                          (implicit system: ActorSystem, s: Scheduler, mat: Materializer) extends Controller with I18nSupport with JSONFormats {

  val solutionForm = Form {
    mapping(
      solution -> nonEmptyText,
      year -> longNumber,
      taskType -> nonEmptyText,
      timeuuid -> nonEmptyText
    )(SolutionForm.apply)(SolutionForm.unapply)
  }

  def getTask(year: Long, taskType: String, timeuuid: UUID) = Action.async { implicit request =>
    def notFound = Redirect(routes.Application.index).flashing(flashToUser -> messagesApi("taskNotFound"))

    val task = TryFuture(getCachedTask(year, taskType, timeuuid))
    task.map {
      case Some(t) => Ok(views.html.task(t.name, t.description,
        solutionForm.fill(SolutionForm(t.solutionTemplate, year, taskType, timeuuid.toString))))
      case None => notFound
    }.recover { case NonFatal(e) => notFound }
  }

  def taskStream = WebSocket.accept { req =>
    ActorFlow.actorRef[JsValue, JsValue] { out =>
      SimpleWebSocketActor.props(out, (fromClient: JsValue) => {
        val prevTimestamp = (fromClient \ "prevTimestamp").as[Long]
        val currentTimestamp = (fromClient \ "currentTimestamp").as[Long]

        if (Duration(currentTimestamp - prevTimestamp, TimeUnit.MILLISECONDS) < 1.seconds) {
          Future(Observable(Line("Too many requests per second from the same client. Slow down")))
        } else {
          val solution = (fromClient \ "solution").as[String]
          val year = (fromClient \ "year").as[Long]
          val taskType = (fromClient \ "taskType").as[String]
          val timeuuid = (fromClient \ "timeuuid").as[String]

          val task = getCachedTask(year, taskType, UUID.fromString(timeuuid))
          task.map { t =>
            if (t.isEmpty) throw new RuntimeException(s"Task is not available for a given solution: $solution")
            ObservableRunner(executor(solution, t.get.suite, t.get.solutionTrait), service.testResult)
          }
        }
      },
        Some(Compiling())
      )
    }
  }

  private def getCachedTask(year: Long, taskType: String, timeuuid: UUID): Future[Option[Task]] = {
    def getFromDb: Future[Option[Task]] = {
      Logger.debug(s"getting task from db: $year, $taskType, $timeuuid")
      TryFuture(dao.getTask(new Date(year), TaskType.withName(taskType), timeuuid))
    }

    val suiteKey = (year, taskType, timeuuid).toString()

    val task = cache.get[Task](suiteKey)
    task match {
      case Some(_) => Future.successful(task)
      case None =>
        val f = getFromDb
        f.foreach {
          case Some(t) => cache.set(suiteKey, t, expiration)
          case None =>
        }
        f
    }
  }

  /**
    * it is an example of executing the loaded test classes into the classPath. We only parse solution text coming from a user
    * Such approach is faster of course, than controllers.TaskSolver#taskStream().
    *
    * This approach can be used for predefined tests of DevGym platform to get better performance for demo tests.
    * Currently, we are not using this method and it should be removed from here to some snippet storage
    *
    * @return WebSocket
    */
  def runtimeTaskStream = WebSocket.accept { req =>
    ActorFlow.actorRef[JsValue, JsValue] { out =>
      SimpleWebSocketActor.props(out, (fromClient: JsValue) =>
        try {
          val suiteClass = "tasktest.SubArrayWithMaxSumTest"
          val solutionTrait = "tasktest.SubArrayWithMaxSumSolution"
          val solution = (fromClient \ "solution").as[String]
          Future.successful(ObservableRunner(executor(
            Class.forName(suiteClass).asInstanceOf[Class[Suite]],
            Class.forName(solutionTrait).asInstanceOf[Class[AnyRef]],
            solution), service.testResult))
        } catch {
          case NonFatal(e) => Future.failed(e)
        },
        Some(Compiling())
      )
    }
  }
}

case class SolutionForm(solution: String, year: Long, taskType: String, timeuuid: String)

object TaskSolver {
  val cannotCheckNow = "cannotCheckNow"
  val solution = "solution"
  val year = "year"
  val taskType = "taskType"
  val timeuuid = "timeuuid"

  val expiration = 60 seconds
}

package controllers

import java.util.{Date, UUID}

import akka.actor.ActorSystem
import akka.stream.Materializer
import com.google.inject.Inject
import controllers.TaskSolver._
import controllers.UserController._
import dal.Dao
import models.{Task, TaskType}
import monifu.concurrent.Scheduler
import org.scalatest.Suite
import play.api.Logger
import play.api.cache.CacheApi
import play.api.data.Form
import play.api.data.Forms._
import play.api.data.validation.{Constraint, Invalid, Valid, ValidationError}
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.libs.json.JsValue
import play.api.libs.streams.ActorFlow
import play.api.mvc.{Action, Controller, WebSocket}
import service._
import shared.Line
import util.TryFuture

import scala.concurrent.Future
import scala.concurrent.duration._
import scala.sys.process._
import scala.util.Try
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
      case Some(t) => Ok(views.html.task(t.description, solutionForm.fill(SolutionForm(t.solutionTemplate, year, taskType, timeuuid.toString))))
      case None => notFound
    }.recover { case NonFatal(e) => notFound }
  }

  def taskStream = WebSocket.accept { req =>
    ActorFlow.actorRef[JsValue, JsValue] { out =>
      SimpleWebSocketActor.props(out, (fromClient: JsValue) => {
          val solution = (fromClient \ "solution").as[String]
          val year = (fromClient \ "year").as[Long]
          val taskType = (fromClient \ "taskType").as[String]
          val timeuuid = (fromClient \ "timeuuid").as[String]
          getCachedTask(year, taskType, UUID.fromString(timeuuid)).map { t =>
            ObservableRunner(executor(solution, t.get.suite, t.get.solutionTrait)).map(Line(_))
          }
        },
        Some(Line("Compiling..."))
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

  def runtimeTaskStream = WebSocket.accept { req =>
    ActorFlow.actorRef[JsValue, JsValue] { out =>
      SimpleWebSocketActor.props(out, (fromClient: JsValue) =>
        try {
          val suiteClass = "tasktest.SubArrayWithMaxSumTest" // (fromClient \ "suiteClass").as[String]
          val solutionTrait = "tasktest.SubArrayWithMaxSumSolution" // (fromClient \ "solutionTrait").as[String]
          val solution = (fromClient \ "solution").as[String]
          Future.successful(ObservableRunner(executor(
            Class.forName(suiteClass).asInstanceOf[Class[Suite]],
            Class.forName(solutionTrait).asInstanceOf[Class[AnyRef]], solution)).map(Line(_)))
        } catch {
          case NonFatal(e) => Future.failed(e)
        },
        Some(Line("Compiling..."))
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

  // TODO rethink or remove
  def nonEmptyAndDiffer(from: String) = nonEmptyText verifying Constraint[String]("changes.required") { o =>
    if (o.filter(_ != '\r') == from) Invalid(ValidationError("error.changesRequired")) else Valid
  }

  def sbt(command: String): Try[Boolean] = Try(Seq("sbt", command).! == 0)

  // no exception, so sbt is in the PATH
  lazy val sbtInstalled = sbt("--version").isSuccess
}

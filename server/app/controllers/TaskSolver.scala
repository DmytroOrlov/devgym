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
import play.api.cache.CacheApi
import play.api.data.Form
import play.api.data.Forms._
import play.api.data.validation.{Constraint, Invalid, Valid, ValidationError}
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.libs.json.JsValue
import play.api.libs.streams.ActorFlow
import play.api.mvc.{AnyContent, Action, Controller, WebSocket}
import play.api.Logger
import service._
import shared.Line
import util.TryFuture

import scala.concurrent.Future
import scala.concurrent.duration._
import scala.sys.process._
import scala.util.control.NonFatal
import scala.util.{Success, Try}

class TaskSolver @Inject()(executor: RuntimeSuiteExecutor with DynamicSuiteExecutor, dao: Dao, val messagesApi: MessagesApi, val cache: CacheApi)
                          (implicit system: ActorSystem, s: Scheduler, mat: Materializer) extends Controller with I18nSupport with JSONFormats {

  val solutionForm = Form {
    mapping(
      solution -> nonEmptyText,
      year -> longNumber,
      taskType -> nonEmptyText,
      timeuuid -> nonEmptyText
    )(SolutionForm.apply)(SolutionForm.unapply)
  }

  def getTask(year: Long, taskType: String, timeuuid: UUID): Action[AnyContent] = Action.async { implicit request =>
    def notFound = Redirect(routes.Application.index).flashing(flashToUser -> messagesApi("taskNotFound"))

    val task = TryFuture(getCachedTask(year, taskType, timeuuid.toString))
    task.map {
      case Some(t) => Ok(views.html.task(t.description, solutionForm.fill(SolutionForm(t.solutionTemplate, year, taskType, timeuuid.toString))))
      case None => notFound
    }.recover { case NonFatal(e) => notFound }
  }

  def taskStream = WebSocket.accept { req =>
    ActorFlow.actorRef[JsValue, JsValue] { out =>
      SimpleWebSocketActor.props(out, (fromClient: JsValue) =>
        try {
          val solution = (fromClient \ "solution").as[String]
          val year = (fromClient \ "year").as[Long]
          val taskType = (fromClient \ "taskType").as[String]
          val timeuuid = (fromClient \ "timeuuid").as[String]
          getCachedTask(year, taskType, timeuuid).map { t =>
            ObservableRunner(executor(solution, t.get.suite)).map(Line(_))
          }
        } catch {
          case NonFatal(e) => Future.failed(e)
        },
        Some(Line("Compiling..."))
      )
    }
  }

  private def getCachedTask(year: Long, taskType: String, timeuuid: String): Future[Option[Task]] = {
    val suiteKey = (year, taskType, timeuuid).toString

    def getFromCache: Option[Task] = Option {
      cache.getOrElse[Task](suiteKey, expiration)(throw new RuntimeException("Cache is empty"))
    }

    def getFromDb: Future[Option[Task]] = {
      Logger.debug(s"getting task from db: $year, $taskType, $timeuuid")
      dao.getTask(new Date(year), TaskType.withName(taskType), UUID.fromString(timeuuid))
    }

    def saveInCache: PartialFunction[Try[Option[Task]], Unit] = {
      case Success(o) => o.foreach(t => cache.set(suiteKey, t))
      case _ =>
    }

    Future (getFromCache) recoverWith {
      case _ => getFromDb andThen saveInCache
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

  val expiration = 15 seconds

  // TODO rethink or remove
  def nonEmptyAndDiffer(from: String) = nonEmptyText verifying Constraint[String]("changes.required") { o =>
    if (o.filter(_ != '\r') == from) Invalid(ValidationError("error.changesRequired")) else Valid
  }

  def sbt(command: String): Try[Boolean] = Try(Seq("sbt", command).! == 0)

  // no exception, so sbt is in the PATH
  lazy val sbtInstalled = sbt("--version").isSuccess
}

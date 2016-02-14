package controllers

import java.util.{Date, UUID}

import com.google.inject.Inject
import controllers.TaskSolver._
import controllers.UserController._
import dal.Dao
import models.TaskType
import monifu.concurrent.Scheduler
import monifu.reactive.Observable
import org.scalatest.Suite
import play.api.Play.current
import play.api.data.Form
import play.api.data.Forms._
import play.api.data.validation.{Constraint, Invalid, Valid, ValidationError}
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.libs.json.JsValue
import play.api.mvc.{Action, Controller, WebSocket}
import service._
import shared.Line
import util.TryFuture

import scala.sys.process._
import scala.util.Try
import scala.util.control.NonFatal

class TaskSolver @Inject()(executor: RuntimeSuiteExecutor, dao: Dao, val messagesApi: MessagesApi)
                          (implicit s: Scheduler) extends Controller with I18nSupport with JSONFormats {
  val appPath = current.path.getAbsolutePath

  val solutionForm = Form {
    mapping(
      solution -> nonEmptyText
    )(SolutionForm.apply)(SolutionForm.unapply)
  }

  def getTask(year: Long, taskType: String, timeuuid: UUID) = Action.async { implicit request =>
    def notFound = Redirect(routes.Application.index).flashing(flashToUser -> messagesApi("taskNotFound"))

    val task = TryFuture(dao.getTask(new Date(year), TaskType.withName(taskType), timeuuid))
    task.map {
      case Some(t) => Ok(views.html.task(t.description, solutionForm.fill(SolutionForm(t.solutionTemplate))))
      case None => notFound
    }.recover { case NonFatal(e) => notFound }
  }

  def taskStream = WebSocket.acceptWithActor[JsValue, JsValue] { req => out =>
    SimpleWebSocketActor.props(out, (fromClient: JsValue) => (try {
        val suiteClass = "tasktest.SubArrayWithMaxSumTest" // (fromClient \ "suiteClass").as[String]
        val solutionTrait = "tasktest.SubArrayWithMaxSumSolution" // (fromClient \ "solutionTrait").as[String]
        val solution = (fromClient \ "solution").as[String]
        ObservableRunner(executor(
          Class.forName(suiteClass).asInstanceOf[Class[Suite]],
          Class.forName(solutionTrait).asInstanceOf[Class[AnyRef]], solution))
      } catch {
        case NonFatal(e) => Observable.error(new SolverException(s"Cannot run ${e.getMessage}"))
      }).map(Line(_)),
      Some(Line("Compiling..."))
    )
  }
}

case class SolutionForm(solution: String)

object TaskSolver {
  val cannotCheckNow = "cannotCheckNow"
  val solution = "solution"

  // TODO rethink or remove
  def nonEmptyAndDiffer(from: String) = nonEmptyText verifying Constraint[String]("changes.required") { o =>
    if (o.filter(_ != '\r') == from) Invalid(ValidationError("error.changesRequired")) else Valid
  }

  def sbt(command: String): Try[Boolean] = Try(Seq("sbt", command).! == 0)

  lazy val sbtInstalled = sbt("sbtVersion").isSuccess // no exception, so sbt is in the PATH

  case class SolverException(msg: String) extends RuntimeException(msg)
}

package controllers

import javax.inject.Inject

import akka.actor.ActorSystem
import akka.stream.Materializer
import akka.stream.scaladsl.Flow
import controllers.AddTask._
import data.TaskDao
import models.Language._
import models.NewTask
import monix.execution.Scheduler
import monix.execution.Scheduler.Implicits.global
import play.api.Logger
import play.api.data.Form
import play.api.data.Forms._
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.{Action, Controller, WebSocket}
import service.meta.CodeParser
import service.reflection.DynamicSuiteExecutor
import shared.model.{Event, SolutionTemplate, TestResult, TestStatus}

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try
import scala.util.control.NonFatal
import scala.util.matching.Regex

class AddTask @Inject()(dynamicExecutor: DynamicSuiteExecutor, dao: TaskDao, val messagesApi: MessagesApi)
                       (implicit system: ActorSystem, mat: Materializer)
  extends Controller with I18nSupport with JSONFormats {

  val addTaskForm = Form {
    mapping(
      taskName -> nonEmptyText,
      taskDescription -> nonEmptyText,
      solutionTemplate -> nonEmptyText,
      referenceSolution -> nonEmptyText,
      suite -> nonEmptyText
    )(AddTaskForm.apply)(AddTaskForm.unapply)
  }

  def getAddTask = Action { implicit request =>
    Ok(views.html.addTask(addTaskForm))
  }

  def postNewTask = Action.async { implicit request =>

    def addTaskViewWithError(errorKey: String, message: String = "", ex: Option[Throwable] = None) = {
      ex.foreach(e => Logger.error(e.getMessage, e))
      views.html.addTask(addTaskForm.bindFromRequest(), Some(s"${messagesApi(errorKey)} $message"))
    }

    addTaskForm.bindFromRequest.fold(
      errorForm => {
        Future.successful(BadRequest(views.html.addTask(errorForm)))
      },
      f => {
        def addTaskIfValid(traitName: String) = {
          val (checkNext, getTestResult) = service.testSync

          val block: (String => Unit) => Unit = dynamicExecutor(f.referenceSolution, f.suite, traitName)
          val testResult: Event = getTestResult(block(checkNext))

          def serverError = InternalServerError {
            views.html.addTask(addTaskForm.bindFromRequest()
              .withError(taskDescription, messagesApi(cannotAddTaskToDatabase)))
          }

          testResult match {
            case t: TestResult => t.testStatus match {
              case TestStatus.Passed | TestStatus.FailedByTest =>
                dao.addTask(NewTask(scalaLang, f.name, f.description, f.solutionTemplate, f.referenceSolution, f.suite, traitName))
                  .map(_ => Redirect(routes.AddTask.getAddTask).flashing(flashToUser -> messagesApi(taskAdded)))
                  .recover {
                    case NonFatal(e) =>
                      Logger.warn(e.getMessage, e)
                      serverError
                  }
              case TestStatus.FailedByCompilation =>
                Future.successful(BadRequest(addTaskViewWithError(cannotAddTaskOnCheck, t.errorMessage)))
            }
            case _ => Future.successful(serverError)
          }
        }

        val checkTrait = Future.fromTry(Try(findTraitName(f.suite)))

        (for {
          traitName <- checkTrait errorMsg addTaskErrorOnSolutionTrait
          result <- addTaskIfValid(traitName) errorMsg cannotAddTaskToDatabase
        } yield result).recover {
          case NonFatal(e) => BadRequest {
            addTaskViewWithError(e.getMessage, ex = Some(e))
          }
        }
      }
    )
  }

  def getSolutionTemplate: WebSocket = WebSocket.accept { req =>
    def getTemplate(jsValue: JsValue) = Json.toJson(
      SolutionTemplate(
        Try(CodeParser.getSolutionTemplate((jsValue \ "solution").as[String]))
          .toOption
          .getOrElse("can't parse code")
      )
    )

    Flow[JsValue].map(getTemplate)
  }

}

case class AddTaskForm(name: String, description: String, solutionTemplate: String, referenceSolution: String, suite: String)

object AddTask {
  val taskName = "taskName"
  val taskDescription = "taskDescription"
  val solutionTemplate = "solutionTemplate"
  val referenceSolution = "referenceSolution"
  val suite = "suite"
  val cannotAddTaskToDatabase = "cannotAddTaskToDatabase"
  val cannotAddTaskOnCheck = "cannotAddTaskOnCheck"
  val addTaskErrorOnSolutionTrait = "addTaskErrorOnSolutionTrait"
  val taskAdded = "taskAdded"

  val traitDefPattern: Regex = """trait\s*([\w\$]*)""".r

  def findTraitName(suite: String) = traitDefPattern.findFirstIn(suite).get.split( """\s+""")(1)

  implicit class ErrorMessageFuture[A](val future: Future[A]) extends AnyVal {
    def errorMsg(messageKey: String)(implicit s: ExecutionContext): Future[A] = future.recoverWith {
      case t: Throwable => Future.failed(new Exception(messageKey, t))
    }
  }

}

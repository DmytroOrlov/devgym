package controllers

import javax.inject.Inject

import akka.actor.ActorSystem
import akka.stream.Materializer
import controllers.AddTask._
import dal.TaskDao
import models.Language._
import models.NewTask
import monifu.concurrent.Scheduler
import monifu.reactive.OverflowStrategy.DropOld
import monifu.reactive.channels.PublishChannel
import play.api.Logger
import play.api.data.Form
import play.api.data.Forms._
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.libs.json.JsValue
import play.api.libs.streams.ActorFlow
import play.api.mvc.{Action, Controller, Request, WebSocket}
import service.meta.CodeParser
import service.reflection.DynamicSuiteExecutor
import service.{StringBuilderRunner, _}
import shared.model.{Compiling, SolutionTemplate, TestStatus}
import util.TryFuture._

import scala.concurrent.Future
import scala.util.control.NonFatal
import scala.util.matching.Regex
import scala.util.{Success, Try}

class AddTask @Inject()(executor: DynamicSuiteExecutor, dao: TaskDao, val messagesApi: MessagesApi)
                       (implicit system: ActorSystem, s: Scheduler, mat: Materializer)
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

  def getAddTask = Action { implicit request: Request[_] =>
    Ok(views.html.addTask(addTaskForm))
  }

  def postNewTask = Action.async { implicit request: Request[_] =>

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
          val testResultOpt = (r: Try[String]) => Option(service.testResult(r))
          val result = StringBuilderRunner(executor(f.referenceSolution, f.suite, traitName), testResultOpt)
          val tR = testResult(Success(result))

          tR.testStatus match {
            case TestStatus.Passed | TestStatus.FailedByTest =>
              dao.addTask(NewTask(scalaLang, f.name, f.description, f.solutionTemplate, f.referenceSolution, f.suite, traitName))
                .map(_ => Redirect(routes.AddTask.getAddTask).flashing(flashToUser -> messagesApi(taskAdded)))
                .recover {
                  case NonFatal(e) => Logger.warn(e.getMessage, e)
                    InternalServerError {
                      views.html.addTask(addTaskForm.bindFromRequest()
                        .withError(taskDescription, messagesApi(cannotAddTaskToDatabase)))
                    }
                }
            case TestStatus.FailedByCompilation => Future {
              BadRequest {
                addTaskViewWithError(cannotAddTaskOnCheck, tR.errorMessage)
              }
            }
          }
        }

        val checkTrait = Try(findTraitName(f.suite)).toFuture

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

  def getSolutionTemplate = WebSocket.accept { req =>
    ActorFlow.actorRef[JsValue, JsValue] { out =>
      SimpleWebSocketActor.props(out, (fromClient: JsValue) => {
        val solution = (fromClient \ "solution").as[String]
        val channel = PublishChannel[SolutionTemplate](DropOld(20))

        Future {
          val template = CodeParser.getSolutionTemplate(solution)
          println(s"template:\n$template")
          channel.pushNext(SolutionTemplate(template))
        }

        Future.successful(channel)
      }
      )
    }
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
    def errorMsg(messageKey: String)(implicit s: Scheduler): Future[A] = future.recoverWith {
      case t: Throwable => Future.failed(new Exception(messageKey, t))
    }
  }

}

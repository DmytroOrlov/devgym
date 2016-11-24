package controllers

import com.google.inject.Inject
import controllers.AddTask._
import dal.TaskDao
import models.Language._
import models.NewTask
import monifu.concurrent.Scheduler
import play.api.Logger
import play.api.data.Form
import play.api.data.Forms._
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, Controller}
import service.reflection.DynamicSuiteExecutor
import service.{StringBuilderRunner, _}
import shared.model.TestStatus
import util.TryFuture._

import scala.concurrent.Future
import scala.util.control.NonFatal
import scala.util.{Success, Try}

class AddTask @Inject()(executor: DynamicSuiteExecutor, dao: TaskDao, val messagesApi: MessagesApi)
                       (implicit s: Scheduler) extends Controller with I18nSupport {
  val addTaskForm = Form {
    mapping(
      taskName -> nonEmptyText,
      taskDescription -> nonEmptyText,
      solutionTemplate -> nonEmptyText,
      referenceSolution -> nonEmptyText,
      suite -> nonEmptyText
    )(AddTaskForm.apply)(AddTaskForm.unapply)
  }

  def getAddTask = Action { implicit r =>
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
          val testResultOpt = (r: Try[String]) => Option(service.testResult(r))
          val result = StringBuilderRunner(executor(f.referenceSolution, f.suite, traitName), testResultOpt)
          val tR = testResult(Success(result))

          tR.testStatus match {
            case TestStatus.Passed =>
              dao.addTask(NewTask(scalaLang, f.name, f.description, f.solutionTemplate, f.referenceSolution, f.suite, traitName))
                .map(_ => Redirect(routes.AddTask.getAddTask).flashing(flashToUser -> messagesApi(taskAdded)))
                .recover {
                  case NonFatal(e) => Logger.warn(e.getMessage, e)
                    InternalServerError {
                      views.html.addTask(addTaskForm.bindFromRequest().withError(taskDescription, messagesApi(cannotAddTask)))
                    }
                }
            case TestStatus.Failed => Future {
              BadRequest {
                addTaskViewWithError(cannotAddTaskOnCheck, tR.errorMessage)
              }
            }
          }
        }

        val checkTrait = Try(findTraitName(f.suite)).toFuture
        //TODO: split error messages: 1) trait check; 2) cannot be saved
        checkTrait.flatMap { traitName =>
          addTaskIfValid(traitName)
        }.recover {
          case NonFatal(e) => BadRequest {
            addTaskViewWithError(addTaskErrorOnSolutionTrait, "Task cannot be saved", Some(e))
          }
        }
      }
    )
  }
}

case class AddTaskForm(name: String, description: String, solutionTemplate: String, referenceSolution: String, suite: String)

object AddTask {
  val taskName = "taskName"
  val taskDescription = "taskDescription"
  val solutionTemplate = "solutionTemplate"
  val referenceSolution = "referenceSolution"
  val suite = "suite"
  val cannotAddTask = "cannotAddTask"
  val cannotAddTaskOnCheck = "cannotAddTaskOnCheck"
  val addTaskErrorOnSolutionTrait = "addTaskErrorOnSolutionTrait"
  val taskAdded = "taskAdded"

  val traitDefPattern = """trait\s*([\w\$]*)""".r

  def findTraitName(suite: String) = traitDefPattern.findFirstIn(suite).get.split( """\s+""")(1)
}

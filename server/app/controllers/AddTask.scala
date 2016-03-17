package controllers

import com.google.inject.Inject
import controllers.AddTask._
import controllers.UserController._
import dal.Dao
import models.NewTask
import models.TaskType._
import monifu.concurrent.Scheduler
import play.api.Logger
import play.api.data.Form
import play.api.data.Forms._
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, Controller}
import service._

import scala.concurrent.Future
import scala.util.control.NonFatal

class AddTask @Inject()(executor: DynamicSuiteExecutor, dao: Dao, val messagesApi: MessagesApi)
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
    addTaskForm.bindFromRequest.fold(
      errorForm => {
        Future.successful(BadRequest(views.html.addTask(errorForm)))
      },
      f => {
        val check = Future(StringBuilderRunner(executor(f.referenceSolution, f.suite))).check
        check.flatMap { _ =>
          dao.addTask(NewTask(scalaClass, f.name, f.description, f.solutionTemplate, f.referenceSolution, f.suite))
            .map(_ => Redirect(routes.AddTask.getAddTask).flashing(flashToUser -> messagesApi(taskAdded)))
            .recover {
              case NonFatal(e) => Logger.warn(e.getMessage, e)
                InternalServerError {
                  views.html.addTask(addTaskForm.bindFromRequest().withError(taskDescription, messagesApi(cannotAddTask)))
                }
            }
        }.recover {
          case e: SuiteException => BadRequest {
            views.html.addTask(addTaskForm.bindFromRequest(), Some(s"${messagesApi(cannotAddTaskOnCheck)} ${e.msg}"))
          }
          case NonFatal(e) => BadRequest {
            Logger.error(e.getMessage, e)
            views.html.addTask(addTaskForm.bindFromRequest(), Some(s"${messagesApi(cannotAddTaskOnCheck)}"))
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
  val taskAdded = "taskAdded"
}

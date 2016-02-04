package controllers

import com.google.inject.Inject
import controllers.NewTask._
import dal.Repo
import models.Task
import models.TaskType._
import play.api.Logger
import play.api.data.Form
import play.api.data.Forms._
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, Controller}

import scala.concurrent.{ExecutionContext, Future}
import scala.util.control.NonFatal

class NewTask @Inject()(repo: Repo, val messagesApi: MessagesApi)
                       (implicit ec: ExecutionContext) extends Controller with I18nSupport {
  val addTaskForm = Form {
    mapping(
      taskDescription -> nonEmptyText,
      solutionTemplate -> nonEmptyText,
      referenceSolution -> nonEmptyText,
      suite -> nonEmptyText
    )(AddTaskForm.apply)(AddTaskForm.unapply)
  }

  def getAddTask = Action(Ok(views.html.addTask(addTaskForm)))

  def postNewTask = Action.async { implicit request =>
    addTaskForm.bindFromRequest.fold(
      errorForm => {
        Future.successful(BadRequest(views.html.addTask(errorForm)))
      },
      f => {
        repo.addTask(Task(scalaClass, f.taskDescription, f.solutionTemplate, f.referenceSolution, f.suite)).map { _ =>
          Redirect(routes.Application.index)
        }.recover {
          case NonFatal(e) => Logger.warn(e.getMessage, e)
            BadRequest(views.html.addTask(addTaskForm.bindFromRequest().withError(taskDescription, messagesApi(cannotAddTask))))
        }
      }
    )
  }
}

case class AddTaskForm(taskDescription: String, solutionTemplate: String, referenceSolution: String, suite: String)

object NewTask {
  val taskDescription = "taskDescription"
  val solutionTemplate = "solutionTemplate"
  val referenceSolution = "referenceSolution"
  val suite = "suite"
  val cannotAddTask = "cannotAddTask"
}

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
import service.ScalaTestRunner

import scala.concurrent.{ExecutionContext, Future}
import scala.util.control.NonFatal

class NewTask @Inject()(repo: Repo, app: play.api.Application, val messagesApi: MessagesApi)
                       (implicit ec: ExecutionContext) extends Controller with I18nSupport {
  val addTaskForm = Form {
    mapping(
      taskDescription -> nonEmptyText,
      solutionHeader -> nonEmptyText,
      solutionBody -> nonEmptyText,
      solutionFooter -> nonEmptyText,
      test -> nonEmptyText
    )(AddTaskForm.apply)(AddTaskForm.unapply)
  }

  def getAddTask = Action(Ok(views.html.addTask(addTaskForm)))

  def postNewTask = Action.async { implicit request =>
    def badTask = BadRequest(
      views.html.addTask(addTaskForm.bindFromRequest().withError(taskDescription, messagesApi(cannotAddTask)))
    )
    addTaskForm.bindFromRequest.fold(
      errorForm => {
        Future.successful(BadRequest(views.html.addTask(errorForm)))
      },
      f => {
        val futureResponse = for {
          test <- Future(ScalaTestRunner.execSuite(f.solutionHeader + f.solutionBody + f.solutionFooter, f.test)) // todo vaidate test output
          db <- repo.addTask(Task(scalaClass, f.taskDescription, f.solutionHeader + f.solutionFooter, f.test))
        } yield Redirect(routes.Application.index)

        futureResponse.recover {
          case NonFatal(e) => Logger.warn(e.getMessage, e)
            badTask
        }
      }
    )
  }
}

case class AddTaskForm(taskDescription: String, solutionHeader: String, solutionBody: String, solutionFooter: String, test: String)

object NewTask {
  val taskDescription = "taskDescription"
  val solutionHeader = "solutionHeader"
  val solutionBody = "solutionBody"
  val solutionFooter = "solutionFooter"
  val test = "test"
  val cannotAddTask = "cannotAddTask"
}

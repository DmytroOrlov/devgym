package controllers

import com.google.inject.Inject
import controllers.NewTask._
import dal.Dao
import models.Task
import models.TaskType._
import play.api.Logger
import play.api.data.Form
import play.api.data.Forms._
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, Controller}
import service.DynamicSuiteExecutor

import scala.concurrent.{ExecutionContext, Future}
import scala.util.control.NonFatal
import scala.util.{Failure, Success}

class NewTask @Inject()(dynamicExecutor: DynamicSuiteExecutor, dao: Dao, val messagesApi: MessagesApi)
                       (implicit ec: ExecutionContext) extends Controller with I18nSupport {
  val addTaskForm = Form {
    mapping(
      taskName -> nonEmptyText,
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
        val testStatus= dynamicExecutor(f.referenceSolution, f.suite, checked = true) match {
          case Success(_) => Future.successful(())
          case Failure(e) => Future.failed(e)
        }
        val futureResponse = for {
          _ <- testStatus
          db <- dao.addTask(Task(scalaClass, f.name, f.description, f.solutionTemplate, f.referenceSolution, f.suite))
        } yield Redirect(routes.NewTask.getAddTask)

        futureResponse.recover {
          case NonFatal(e) => Logger.warn(e.getMessage, e)
            BadRequest {
              views.html.addTask(addTaskForm.bindFromRequest().withError(taskDescription, s"${messagesApi(cannotAddTask)}: ${e.getMessage}"))
            }
        }
      }
    )
  }
}

case class AddTaskForm(name: String, description: String, solutionTemplate: String, referenceSolution: String, suite: String)

object NewTask {
  val taskName = "taskName"
  val taskDescription = "taskDescription"
  val solutionTemplate = "solutionTemplate"
  val referenceSolution = "referenceSolution"
  val suite = "suite"
  val cannotAddTask = "cannotAddTask"
}

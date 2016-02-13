package controllers

import java.sql.Timestamp

import com.datastax.driver.core.utils.UUIDs
import com.google.inject.Inject
import controllers.NewTask._
import dal.Dao
import models.Task
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

class NewTask @Inject()(executor: DynamicSuiteExecutor, dao: Dao, val messagesApi: MessagesApi)
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
        val futureResponse = for {
          _ <- Future(StringBuilderRunner(executor(f.referenceSolution, f.suite))).check
          db <- dao.addTask(
            Task(new Timestamp(System.currentTimeMillis),
              scalaClass, UUIDs.timeBased(), f.name, f.description, f.solutionTemplate, f.referenceSolution, f.suite)
          )
        } yield Redirect(routes.Application.index)

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

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
import util.TryFuture

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
    def addTaskViewWithError(errorKey: String, e: Option[Throwable] = None, message: String = "") = {
      e match {
        case Some(ex) => Logger.error(ex.getMessage, ex)
        case None =>
      }
      views.html.addTask(addTaskForm.bindFromRequest(), Some(s"${messagesApi(errorKey)} $message"))
    }

    addTaskForm.bindFromRequest.fold(
      errorForm => {
        Future.successful(BadRequest(views.html.addTask(errorForm)))
      },
      f => {
        val checkTrait = TryFuture(Future(findTraitName(f.suite)))
        def checkSolution(solutionTrait: String) = Future(StringBuilderRunner(executor(f.referenceSolution, f.suite, solutionTrait))).check

        checkTrait.flatMap { traitName =>
            checkSolution(traitName).flatMap { r =>
              dao.addTask(NewTask(scalaClass, f.name, f.description, f.solutionTemplate, f.referenceSolution, f.suite, traitName))
                .map(_ => Redirect(routes.AddTask.getAddTask).flashing(flashToUser -> messagesApi(taskAdded)))
                .recover {
                  case NonFatal(e) => Logger.warn(e.getMessage, e)
                    InternalServerError {
                      views.html.addTask(addTaskForm.bindFromRequest().withError(taskDescription, messagesApi(cannotAddTask)))
                    }
                }
            }.recover {
              case e: SuiteException => BadRequest {
                addTaskViewWithError(cannotAddTaskOnCheck, Some(e), e.msg)
              }
              case NonFatal(e) => BadRequest {
                addTaskViewWithError(cannotAddTaskOnCheck, Some(e))
              }
            }
        }.recover {
          case NonFatal(e) => BadRequest {
            addTaskViewWithError(addTaskErrorOnSolutionTrait, Some(e))
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
}

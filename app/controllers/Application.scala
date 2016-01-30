package controllers

import com.google.inject.Inject
import com.typesafe.scalalogging.StrictLogging
import controllers.Application._
import controllers.UserController._
import dal.Repo
import models.Task
import models.TaskType._
import org.scalatest.Suite
import play.api.data.Form
import play.api.data.Forms._
import play.api.data.validation.{Constraint, Invalid, Valid, ValidationError}
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc._
import service.ScalaTestRunner

import scala.concurrent._
import scala.sys.process._
import scala.util.Try

class Application @Inject()(repo: Repo, app: play.api.Application, val messagesApi: MessagesApi)(implicit ec: ExecutionContext) extends Controller with I18nSupport with StrictLogging {
  val appPath = app.path.getAbsolutePath

  val solutionForm = Form {
    mapping(
      solution -> nonEmptyAndDirty(original = solutionTemplateText)
    )(SolutionForm.apply)(SolutionForm.unapply)
  }

  val addTaskForm = Form {
    mapping(
      taskDescription -> nonEmptyText,
      solutionTemplate -> nonEmptyText,
      referenceSolution -> nonEmptyText,
      test -> nonEmptyText
    )(AddTaskForm.apply)(AddTaskForm.unapply)
  }

  def index = Action { implicit request =>
    Ok(views.html.index())
  }

  def getTask = Action { implicit request =>
    Ok(views.html.task(taskDescriptionText, solutionForm.fill(SolutionForm(solutionTemplateText))))
  }

  def getAddTask = Action(Ok(views.html.addTask(addTaskForm)))

  def postNewTask = Action.async { implicit request =>
    addTaskForm.bindFromRequest.fold(
      errorForm => {
        Future.successful(BadRequest(views.html.addTask(errorForm)))
      },
      f => {
        repo.addTask(Task(scalaClass, f.taskDescription, f.solutionTemplate, f.referenceSolution, f.test)).map { _ =>
          Redirect(routes.Application.index)
        }.recover {
          case e => logger.warn(e.getMessage, e)
            BadRequest(views.html.addTask(addTaskForm.bindFromRequest().withError(taskDescription, "Cannot add your task now")))
        }
      }
    )
  }

  def postSolution = Action.async { implicit request =>
    solutionForm.bindFromRequest.fold(
      errorForm => {
        Future.successful(BadRequest(views.html.task(taskDescriptionText, errorForm)))
      },
      form => {
        if (sbtInstalled) Future {
          blocking(Ok(testSolution(form.solution, appPath)))
        } else Future.successful {
          BadRequest(
            views.html.task(taskDescriptionText, solutionForm.bindFromRequest().withError(solution, "Cannot check your solution now"))
          )
        }
      })
  }

  def logout = Action { implicit request =>
    val redirectTo = Redirect(routes.Application.index)
    request.session.get(username).fold(redirectTo.withNewSession) { _ =>
      redirectTo
        .withNewSession
        .flashing(flashToUser -> logoutDone)
    }
  }

  def postSolutionAjax(solution: String) = Action {
    // todo add validation for ajax-solution too
    Ok(testSolution(solution, appPath).replaceAll("\n", "<br/>")) //temp solution to have lines in html
  }

  private def testSolution(solution: String, appAbsolutePath: String): String = {
    ScalaTestRunner.execSuite(
      solution,
      Class.forName("tasktest.SubArrayWithMaxSumTest").asInstanceOf[Class[Suite]],
      Class.forName("tasktest.SubArrayWithMaxSumSolution").asInstanceOf[Class[AnyRef]]
    )
    //SbtTestRunner.createProjectAndTest(solution, appAbsolutePath)
  }
}

case class SolutionForm(solution: String)

case class AddTaskForm(taskDescription: String, solutionTemplate: String, referenceSolution: String, test: String)

object Application {
  val logoutDone = "Logout done"
  val solution = "solution"
  val taskDescription = "taskDescription"
  val solutionTemplate = "solutionTemplate"
  val referenceSolution = "referenceSolution"
  val test = "test"

  // these stubs should be replaced with database layer
  val taskDescriptionText = "Implement apply function to return  a sub-array of original array 'a', " +
    "which has maximum sum of its elements.\n For example, " +
    "having such input Array(-2, 1, -3, 4, -1, 2, 1, -5, 4), " +
    "then result should be Array(4, -1, 2, 1), which has maximum sum = 6. You can not rearrange elements of the initial array. \n\n" +
    "You can add required Scala class using regular 'import' statement"
  val solutionTemplateText =
    """class SubArrayWithMaxSum {
      |  def apply(a: Array[Int]): Array[Int] = {
      |
      |  }
      |}""".stripMargin

  def nonEmptyAndDirty(original: String) = nonEmptyText verifying Constraint[String]("changes.required") { o =>
    if (o.filter(_ != '\r') == original) Invalid(ValidationError("error.changesRequired")) else Valid
  }

  def sbt(command: String): Try[Boolean] = Try(Seq("sbt", command).! == 0)

  lazy val sbtInstalled = sbt("sbtVersion").isSuccess // no exception, so sbt is in the PATH
}

package controllers

import com.google.inject.Inject
import controllers.TaskSolver._
import dal.Repo
import dal.Repo.current
import models.TaskType.scalaClass
import org.scalatest.Suite
import play.api.Play
import play.api.data.Form
import play.api.data.Forms._
import play.api.data.validation.{Constraint, Invalid, Valid, ValidationError}
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, Controller}
import service.ScalaTestRunner

import scala.concurrent._
import scala.sys.process._
import scala.util.Try
import scala.util.control.NonFatal

class TaskSolver @Inject()(repo: Repo, val messagesApi: MessagesApi)
                          (implicit ec: ExecutionContext) extends Controller with I18nSupport {
  val appPath = Play.current.path.getAbsolutePath

  val solutionForm = Form {
    mapping(
      solution -> nonEmptyAndDiffer(from = solutionTemplateText)
    )(SolutionForm.apply)(SolutionForm.unapply)
  }

  def getTask = Action { implicit request =>
    Ok(views.html.task(taskDescriptionText, solutionForm.fill(SolutionForm(solutionTemplateText))))
  }

  def postSolution = Action.async { implicit request =>
    val cannotCheckSolution = BadRequest(
      views.html.task(taskDescriptionText, solutionForm.bindFromRequest().withError(solution, messagesApi(cannotCheckNow)))
    )

    solutionForm.bindFromRequest.fold(
      errorForm => {
        Future.successful(BadRequest(views.html.task(taskDescriptionText, errorForm)))
      },
      form => {
        if (!sbtInstalled) Future.successful {
          cannotCheckSolution
        } else Future {
          blocking(Ok(testSolution(form.solution, appPath)))
        }.recover { case NonFatal(e) =>
          cannotCheckSolution
        }
      })
  }

  def postSolutionAjax(solution: String) = Action {
    // todo add validation for ajax-solution too
    Ok(testSolution(solution, appPath).replaceAll("\n", "<br/>")) //temp solution to have lines in html
  }

  def tasks = Action.async(repo.getTasks(scalaClass, 20, current).map(ts => Ok(ts.toString())))

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

object TaskSolver {
  val cannotCheckNow = "cannotCheckNow"
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

  val solution = "solution"

  def nonEmptyAndDiffer(from: String) = nonEmptyText verifying Constraint[String]("changes.required") { o =>
    if (o.filter(_ != '\r') == from) Invalid(ValidationError("error.changesRequired")) else Valid
  }

  def sbt(command: String): Try[Boolean] = Try(Seq("sbt", command).! == 0)

  lazy val sbtInstalled = sbt("sbtVersion").isSuccess // no exception, so sbt is in the PATH
}

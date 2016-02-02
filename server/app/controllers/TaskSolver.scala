package controllers

import com.google.inject.Inject
import controllers.TaskSolver._
import monifu.concurrent.Scheduler
import org.scalatest.Suite
import play.api.Play.current
import play.api.data.Form
import play.api.data.Forms._
import play.api.data.validation.{Constraint, Invalid, Valid, ValidationError}
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.libs.json.JsValue
import play.api.mvc.{Action, Controller, WebSocket}
import service.ScalaTestRunner
import stream.SimpleWebSocketActor

import scala.concurrent._
import scala.sys.process._
import scala.util.Try

class TaskSolver @Inject()(val messagesApi: MessagesApi)
                          (implicit ec: ExecutionContext) extends Controller with I18nSupport with JSONFormats {
  implicit val s = Scheduler(ec)

  def appPath = current.path.getAbsolutePath

  val solutionForm = Form {
    mapping(
      solution -> nonEmptyAndDirty(original = solutionTemplateText)
    )(SolutionForm.apply)(SolutionForm.unapply)
  }

  def getTask = Action { implicit request =>
    Ok(views.html.task(taskDescriptionText, solutionForm.fill(SolutionForm(solutionTemplateText))))
  }

  def taskStream = WebSocket.acceptWithActor[String, JsValue] { req => out =>
    SimpleWebSocketActor.props(out, ScalaTestRunner(
      Class.forName("tasktest.SubArrayWithMaxSumTest").asInstanceOf[Class[Suite]],
      Class.forName("tasktest.SubArrayWithMaxSumSolution").asInstanceOf[Class[AnyRef]]
    ))
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

  def nonEmptyAndDirty(original: String) = nonEmptyText verifying Constraint[String]("changes.required") { o =>
    if (o.filter(_ != '\r') == original) Invalid(ValidationError("error.changesRequired")) else Valid
  }

  def sbt(command: String): Try[Boolean] = Try(Seq("sbt", command).! == 0)

  lazy val sbtInstalled = sbt("sbtVersion").isSuccess // no exception, so sbt is in the PATH
}
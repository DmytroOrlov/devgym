package controllers

import com.google.inject.Inject
import controllers.Application._
import org.scalatest.Suite
import play.api.data.Form
import play.api.data.Forms._
import play.api.data.validation.{Constraint, Invalid, Valid, ValidationError}
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc._
import service.{SbtTestRunner, ScalaTestRunner}

import scala.concurrent._
import scala.sys.process._
import scala.util.Try

class Application @Inject()(app: play.api.Application, val messagesApi: MessagesApi)(implicit ec: ExecutionContext) extends Controller with I18nSupport {

  val probForm = Form {
    mapping(
      prob -> nonEmptyAndDirty(original = blank)
    )(ProbForm.apply)(ProbForm.unapply)
  }

  def index = Action(Redirect(routes.Application.getProb))

  def getProb = Action(Ok(views.html.prob(task, probForm.fill(ProbForm(blank)))))

  def postProb() = Action.async { implicit request =>
    probForm.bindFromRequest.fold(
      errorForm => {
        Future.successful(BadRequest(views.html.prob(task, errorForm)))
      },
      answer => {
        if (!sbtInstalled) Future.successful {
          BadRequest(
            views.html.prob(task, probForm.bindFromRequest().withError(prob, "Cannot test your code now"))
          )
        } else Future(blocking {
          Ok(testSolution(answer.prob, app.path.getAbsolutePath))
        })
      })
  }

  def testSolution(solution: String, appAbsolutePath: String): String = {
    ScalaTestRunner.execSuite(
      solution,
      Class.forName("tasktest.SubArrayWithMaxSumTest").asInstanceOf[Class[Suite]],
      Class.forName("tasktest.SubArrayWithMaxSumSolution").asInstanceOf[Class[AnyRef]]
    )
    //SbtTestRunner.createProjectAndTest(solution, appAbsolutePath)
  }
}

case class ProbForm(prob: String)

object Application {
  val prob = "prob"

  val task = "Implement apply function to return  a sub-array of original array 'a', " +
    "which has maximum sum of its elements.\n For example, " +
    "having such input Array(-2, 1, -3, 4, -1, 2, 1, -5, 4), " +
    "then result should be Array(4, -1, 2, 1), which has maximum sum = 6. You can not rearrange elements of the initial array. \n\n" +
  "You can add required Scala class using regular 'import' statement"

  val blank =
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

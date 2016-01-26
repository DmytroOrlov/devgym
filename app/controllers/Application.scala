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
      "prob" -> nonEmptyAndChanged(original = blank)
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
        val p = Promise[Result]()
        if (!sbtInstalled) p.success {
          BadRequest(
            views.html.prob(task, probForm.bindFromRequest().withError("prob", "Cannot test your code now"))
          )
        } else Future(blocking {
          p.success(Ok(testSolution(answer.prob, app.path.getAbsolutePath)))
        })
        p.future
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
  val task = "The parameter weekday is true if it is a weekday, and the parameter vacation is true if we are on vacation. We sleep in if it is not a weekday or we're on vacation. Return true if we sleep in.\n\nsleepIn(false, false) → true\nsleepIn(true, false) → false\nsleepIn(false, true) → true"

  val blank = "def apply(a: Array[Int]): Array[Int] = {\n  \n}"

  def nonEmptyAndChanged(original: String) = nonEmptyText verifying Constraint[String]("changes.required") { o =>
    if (o.filter(_ != '\r') == original) Invalid(ValidationError("error.changesRequired")) else Valid
  }

  def sbt(command: String): Try[Boolean] = Try(Seq("sbt", command).! == 0)

  lazy val sbtInstalled = sbt("sbtVersion").isSuccess // no exception, so sbt is in the PATH
}

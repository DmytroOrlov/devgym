package controllers

import com.google.inject.Inject
import controllers.Application._
import play.api.data.Form
import play.api.data.Forms._
import play.api.data.validation.{Constraint, Invalid, Valid, ValidationError}
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc._

import scala.concurrent._
import scala.sys.process._
import scala.util.Try

class Application @Inject()(val messagesApi: MessagesApi)(implicit ec: ExecutionContext) extends Controller with I18nSupport {

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
        } else {
          // todo try to avoid Future at all
          Future(blocking {
            val tryTest = Try(Seq("pwd").!!)
            p.success(Ok(tryTest.getOrElse("Test failed")))
          })
        }
        p.future
      })
  }
}

case class ProbForm(prob: String)

object Application {
  val task = "The parameter weekday is true if it is a weekday, and the parameter vacation is true if we are on vacation. We sleep in if it is not a weekday or we're on vacation. Return true if we sleep in.\n\nsleepIn(false, false) → true\nsleepIn(true, false) → false\nsleepIn(false, true) → true"

  val blank = "def sleepIn(weekday: Boolean, vacation: Boolean): Boolean = {\n  \n}"

  def nonEmptyAndChanged(original: String) = nonEmptyText verifying Constraint[String]("changes.required") { o =>
    if (o.filter(_ != '\r') == original) Invalid(ValidationError("error.changesRequired")) else Valid
  }

  def sbt(command: String): Try[Boolean] = Try(Seq("sbt", command).! == 0)

  lazy val sbtInstalled = sbt("--version").isSuccess // no exception, so sbt is in the PATH
}

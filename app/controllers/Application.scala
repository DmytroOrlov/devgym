package controllers

import com.google.inject.Inject
import controllers.Application._
import play.api.data.Form
import play.api.data.Forms._
import play.api.data.validation.{Constraint, Invalid, Valid, ValidationError}
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc._

import scala.concurrent.Future

class Application @Inject()(val messagesApi: MessagesApi) extends Controller with I18nSupport {

  val probForm = Form {
    mapping(
      "prob" -> nonEmptyAndChanged(original = blank)
    )(ProbForm.apply)(ProbForm.unapply)
  }

  def nonEmptyAndChanged(original: String) = nonEmptyText verifying Constraint[String]("changes.required") { o =>
    if (o.filter(_ != '\r') == original) Invalid(ValidationError("error.changesRequired")) else Valid
  }

  def index = Action(Redirect(routes.Application.getProb))

  def getProb = Action(Ok(views.html.prob(task, probForm.fill(ProbForm(blank)))))

  def postProb() = Action.async { implicit request =>
    probForm.bindFromRequest.fold(
      errorForm => {
        Future.successful(BadRequest(views.html.prob(task, errorForm)))
      },
      answer => {
        runTest(answer.prob)
      })
  }

  def runTest(answerCode: String): Future[Result] = ???
}

case class ProbForm(prob: String)

object Application {
  val task = "The parameter weekday is true if it is a weekday, and the parameter vacation is true if we are on vacation. We sleep in if it is not a weekday or we're on vacation. Return true if we sleep in.\n\nsleepIn(false, false) → true\nsleepIn(true, false) → false\nsleepIn(false, true) → true"

  val blank = "public boolean sleepIn(boolean weekday, boolean vacation) {\n  \n}"
}

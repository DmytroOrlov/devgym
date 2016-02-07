package controllers

import com.google.inject.Inject
import controllers.Application._
import controllers.UserController._
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc._

class Application @Inject()(val messagesApi: MessagesApi) extends Controller with I18nSupport {

  def index = Action { implicit request =>
    Ok(views.html.index(tests))
  }

  def logout = Action { request =>
    val redirect = Redirect(routes.Application.index)
    request.session.get(username).fold(redirect.withNewSession) { _ =>
      redirect
        .withNewSession
        .flashing(flashToUser -> messagesApi(logoutDone))
    }
  }
}

object Application {
  val logoutDone = "logoutDone"
  val tests = List("Sub-array with max sum", "Unique numbers in array")
}

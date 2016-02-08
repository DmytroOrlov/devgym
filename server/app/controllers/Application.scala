package controllers

import com.google.inject.Inject
import controllers.Application._
import controllers.UserController._
import dal.Dao
import dal.Dao._
import models.TaskType
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc._

import scala.concurrent.ExecutionContext

class Application @Inject()(dao: Dao, val messagesApi: MessagesApi)(implicit ec: ExecutionContext) extends Controller with I18nSupport {

  def index = Action.async { implicit request =>
    val tasks = dao.getTasks(TaskType.scalaClass, 20, now)
    tasks.map(it => Ok(views.html.index(it)))
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
}

package controllers

import javax.inject.Inject

import controllers.Application._
import data.TaskDao
import models.Language.scalaLang
import models.Task
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc._

import scala.concurrent.ExecutionContext
import scala.util.control.NonFatal

class Application @Inject()(dao: TaskDao, val messagesApi: MessagesApi)(implicit ec: ExecutionContext) extends Controller with I18nSupport {

  def index = Action.async { implicit request =>
    val tasks = dao.getTasks(scalaLang, lastCount, Task.now)
    tasks
      .map(it => Ok(views.html.index(it)))
      .recover {
        case NonFatal(e) => InternalServerError(views.html.index(Seq()))
      }
  }

  def logout = Action { implicit request =>
    val redirect = Redirect(routes.Application.index)
    request.session.get(loginName).fold(redirect.withNewSession) { _ =>
      redirect
        .withNewSession
        .flashing(flashToUser -> messagesApi(logoutDone))
    }
  }
}

object Application {
  val logoutDone = "logoutDone"
  val lastCount = 20
}

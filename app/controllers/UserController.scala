package controllers

import java.security.MessageDigest

import com.google.inject.Inject
import com.typesafe.scalalogging.StrictLogging
import controllers.UserController._
import dal.Repo
import models.User
import play.api.data.Forms._
import play.api.data.{Form, FormError}
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc._
import sun.misc.BASE64Encoder

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Random
import scala.util.control.NonFatal

class UserController @Inject()(repo: Repo, val messagesApi: MessagesApi)(implicit ec: ExecutionContext) extends Controller with I18nSupport with StrictLogging {

  val registerForm: Form[RegisterForm] = Form {
    mapping(
      name -> nonEmptyText,
      password -> nonEmptyText,
      verify -> nonEmptyText
    )(RegisterForm.apply)(RegisterForm.unapply) verifying(passwordsNotMatched, validatePassword _)
  }

  def getRegister = Action { implicit request =>
    request.session.get(username).fold(Ok(views.html.register(registerForm))) { _ =>
      Redirect(routes.Application.index)
        .flashing(flashToUser -> messagesApi(alreadyRegistered))
    }
  }

  def postRegister() = Action.async { implicit request =>
    def nameBusy = Ok(views.html.register(registerForm.bindFromRequest
      .withError(name, messagesApi(nameRegistered))))

    registerForm.bindFromRequest.fold(
      errorForm => {
        Future.successful(BadRequest(views.html.register(withPasswordMatchError(errorForm))))
      },
      form => {
        val hash = passwordHash(form.password, Random.nextInt().toString)
        repo.create(User(form.name, hash)).map { r =>
          if (r.one().getBool(applied)) Redirect(routes.Application.index)
            .withSession(username -> form.name)
            .flashing(flashToUser -> messagesApi(userRegistered))
          else nameBusy
        }.recover {
          case NonFatal(e) => logger.warn(e.getMessage, e)
            nameBusy
        }
      }
    )
  }

  def withPasswordMatchError(errorForm: Form[RegisterForm]) =
    if (errorForm.errors.collectFirst({ case FormError(_, List(`passwordsNotMatched`), _) => true }).nonEmpty)
      errorForm.withError(password, messagesApi(passwordsNotMatched))
    else errorForm
}

case class RegisterForm(name: String, password: String, verify: String)

object UserController {
  val username = "username"
  val flashToUser = "flashToUser"

  val name = "name"
  val password = "password"
  val verify = "verify"
  val applied = "[applied]"

  val userRegistered = "userRegistered"
  val alreadyRegistered = "alreadyRegistered"
  val nameRegistered = "nameRegistered"
  val passwordsNotMatched = "passwordsNotMatched"

  def validatePassword(f: RegisterForm) = f.password.equals(f.verify)

  val encoder: BASE64Encoder = new BASE64Encoder

  def passwordHash(password: String, salt: String) = {
    val saltedAndHashed: String = password + "," + salt
    val digest: MessageDigest = MessageDigest.getInstance("MD5")
    digest.update(saltedAndHashed.getBytes)
    val hashedBytes = new String(digest.digest, "UTF-8").getBytes
    encoder.encode(hashedBytes) + "," + salt
  }
}

package controllers

import java.security.MessageDigest

import com.google.inject.Inject
import controllers.UserController._
import dal.Repo
import models.User
import play.api.Logger
import play.api.data.Forms._
import play.api.data.{Form, FormError}
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc._
import sun.misc.BASE64Encoder

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Random
import scala.util.control.NonFatal

class UserController @Inject()(repo: Repo, val messagesApi: MessagesApi)(implicit ec: ExecutionContext) extends Controller with I18nSupport {

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

    def withPasswordMatchError(errorForm: Form[RegisterForm]) =
      if (errorForm.errors.collectFirst({ case FormError(_, List(`passwordsNotMatched`), _) => true }).nonEmpty)
        errorForm.withError(password, messagesApi(passwordsNotMatched))
      else errorForm

    registerForm.bindFromRequest.fold(
      errorForm => {
        Future.successful(BadRequest(views.html.register(withPasswordMatchError(errorForm))))
      },
      form => {
        val hashSalt = toHashSalt(form.password, Random.nextInt().toString) match { case (h, s) => combine(h, s)}
        repo.create(User(form.name, hashSalt)).map { r =>
          if (r) Redirect(routes.Application.index)
            .withSession(username -> form.name)
            .flashing(flashToUser -> messagesApi(userRegistered))
          else nameBusy
        }.recover {
          case NonFatal(e) => Logger.warn(e.getMessage, e)
            nameBusy
        }
      }
    )
  }
}

case class RegisterForm(name: String, password: String, verify: String)

object UserController {
  val username = "username"
  val flashToUser = "flashToUser"

  val name = "name"
  val password = "password"
  val verify = "verify"

  val userRegistered = "userRegistered"
  val alreadyRegistered = "alreadyRegistered"
  val nameRegistered = "nameRegistered"
  val passwordsNotMatched = "passwordsNotMatched"

  def validatePassword(f: RegisterForm) = f.password.equals(f.verify)

  val encoder: BASE64Encoder = new BASE64Encoder

  def toHashSalt(password: String, salt: String) = {
    val withSalt = combine(password, salt)
    val digest = MessageDigest.getInstance("MD5")
    digest.update(withSalt.getBytes)
    val hashedBytes = new String(digest.digest, "UTF-8").getBytes
    encoder.encode(hashedBytes) -> salt
  }

  def combine(password: String, salt: String) = password + "," + salt
}

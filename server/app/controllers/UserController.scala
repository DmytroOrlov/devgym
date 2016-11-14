package controllers

import java.security.MessageDigest

import com.google.inject.Inject
import controllers.UserController._
import dal.Dao
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

class UserController @Inject()(dao: Dao, val messagesApi: MessagesApi)(implicit ec: ExecutionContext) extends Controller with I18nSupport {

  val registerForm: Form[RegisterForm] = Form {
    mapping(
      name -> nonEmptyText,
      password -> nonEmptyText,
      verify -> nonEmptyText
    )(RegisterForm.apply)(RegisterForm.unapply) verifying(passwordsNotMatched, validatePassword _)
  }

  val loginForm: Form[LoginForm] = Form {
    mapping(
      name -> nonEmptyText,
      password -> nonEmptyText
    )(LoginForm.apply)(LoginForm.unapply)
  }

  def getRegister = Action { implicit request =>
    request.session.get(user).fold(Ok(views.html.register(registerForm))) { _ =>
      Redirect(routes.Application.index)
        .flashing(flashToUser -> messagesApi(alreadyRegistered))
    }
  }

  def postRegister = Action.async { implicit request =>
    def nameBusy = BadRequest(views.html.register(registerForm.bindFromRequest
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
        val hashSalt = toHashSalt(form.password, Random.nextInt().toString)
        dao.create(User(form.name, hashSalt)).map { applied =>
          if (applied) Redirect(routes.Application.index)
            .withSession(user -> form.name)
            .flashing(flashToUser -> messagesApi(userRegistered))
          else nameBusy
        }.recover {
          case NonFatal(e) => Logger.warn(e.getMessage, e)
            nameBusy
        }
      }
    )
  }

  def getLogin = Action { implicit request =>
    request.session.get(user).fold(Ok(views.html.login(loginForm))) { _ =>
      Redirect(routes.Application.index).flashing(flashToUser -> messagesApi(alreadyLoggedin))
    }
  }

  def postLogin = Action.async { implicit request =>
    loginForm.bindFromRequest.fold(
      errorForm => {
        Future.successful(BadRequest(views.html.login(errorForm)))
      },
      f => {
        def passwordsMatch(userPassword: String, formPassword: String) = {
          userPassword == toHashSalt(formPassword, getSalt(userPassword))
        }
        dao.find(f.name).map {
          case Some(u) if passwordsMatch(u.password, f.password) =>
            Redirect(routes.Application.index)
              .withSession(user -> f.name)
          case _ => BadRequest(views.html.login(loginForm.bindFromRequest()
            .withError(name, messagesApi(nameOrPasswordsNotMatched))))
        }
      }
    )
  }

}

case class RegisterForm(name: String, password: String, verify: String)

case class LoginForm(name: String, password: String)

object UserController {
  val flashToUser = "flashToUser"

  val name = "name"
  val password = "password"
  val verify = "verify"

  val userRegistered = "userRegistered"
  val alreadyRegistered = "alreadyRegistered"
  val alreadyLoggedin = "alreadyLoggedin"
  val nameRegistered = "nameRegistered"
  val passwordsNotMatched = "passwordsNotMatched"
  val nameOrPasswordsNotMatched = "nameOrPasswordsNotMatched"

  def validatePassword(f: RegisterForm) = f.password.equals(f.verify)

  val encoder: BASE64Encoder = new BASE64Encoder

  def toHashSalt(password: String, salt: String) = {
    val withSalt = combine(password, salt)
    val digest = MessageDigest.getInstance("MD5")
    digest.update(withSalt.getBytes)
    val hashedBytes = new String(digest.digest, "UTF-8").getBytes
    combine(encoder.encode(hashedBytes), salt)
  }

  def combine(password: String, salt: String) = password + "," + salt

  def getSalt(password: String) = password.split(",")(1)
}

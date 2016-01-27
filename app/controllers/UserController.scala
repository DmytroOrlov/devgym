package controllers

import java.security.MessageDigest

import com.google.inject.Inject
import com.typesafe.scalalogging.StrictLogging
import dal.Repo
import models.User
import play.api.data.{FormError, Form}
import play.api.data.Forms._
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc._
import controllers.UserController._
import sun.misc.BASE64Encoder

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Random

class UserController @Inject()(repo: Repo, val messagesApi: MessagesApi)(implicit ec: ExecutionContext) extends Controller with I18nSupport with StrictLogging {

  val registerForm: Form[RegisterForm] = Form {
    mapping(
      "name" -> nonEmptyText,
      "password" -> nonEmptyText,
      "verify" -> nonEmptyText
    )(RegisterForm.apply)(RegisterForm.unapply) verifying(passwordsNotMatched, validatePassword _)
  }

  def getRegister = Action { implicit request =>
    request.session.get(username).fold(Ok(views.html.register(registerForm))) { _ =>
      Redirect(routes.Application.index)
        .flashing(flashToUser -> alreadyRegistered)
    }
  }

  def postRegister() = Action.async { implicit request =>
    registerForm.bindFromRequest.fold(
      errorForm => {
        Future.successful(BadRequest(views.html.register(withPasswordMatchError(errorForm))))
      },
      user => {
        val hash = passwordHash(user.password, Random.nextInt().toString)
        repo.create(User(user.name, hash)).map { _ =>
          Redirect(routes.Application.index)
            .withSession(username -> user.name)
            .flashing(flashToUser -> userRegistered)
        }.recover {
          case e => logger.warn(e.getMessage, e)
            Ok(views.html.register(registerForm.bindFromRequest
              .withError("name", nameRegistered)))
        }
      }
    )
  }

  def withPasswordMatchError(errorForm: Form[RegisterForm]) =
    if (errorForm.errors.collectFirst({ case FormError(_, List(`passwordsNotMatched`), _) => true }).nonEmpty)
      errorForm.withError("password", passwordsNotMatched)
    else errorForm
}

case class RegisterForm(name: String, password: String, verify: String)

object UserController {
  val username = "username"
  val flashToUser = "flashToUser"

  val userRegistered = "Thank you for your registration"
  val alreadyRegistered = "You are already registered"
  val nameRegistered = "Name already registered, Please choose another"
  val passwordsNotMatched = "Passwords not matched"

  def validatePassword(f: RegisterForm) = f.password.equals(f.verify)

  def passwordHash(password: String, salt: String) = {
    val saltedAndHashed: String = password + "," + salt
    val digest: MessageDigest = MessageDigest.getInstance("MD5")
    digest.update(saltedAndHashed.getBytes)
    val encoder: BASE64Encoder = new BASE64Encoder
    val hashedBytes = new String(digest.digest, "UTF-8").getBytes
    encoder.encode(hashedBytes) + "," + salt
  }
}

package controllers

import java.nio.charset.Charset
import java.nio.file._
import java.nio.file.attribute.BasicFileAttributes

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
          import scala.collection.JavaConversions._
          p.success(Ok {
            Try {
              val lines = List(
                """lazy val root = (project in file("."))""",
                """  .settings(""",
                """    scalaVersion := "2.11.7",""",
                """    libraryDependencies += "org.scalatest" %% "scalatest" % "2.2.6" % "test"""",
                """  )"""
              )
              val dir = Files.createTempDirectory("test")
              Files.write(dir.resolve("build.sbt"), lines, Charset.forName("UTF-8"))
              dir
            }.flatMap { d =>
              Try {
                Files.createDirectory(d.resolve("project"))
                d
              }
            }.flatMap { d =>
              Try {
                Files.write(d.resolve("project").resolve("build.properties"), List("sbt.version=0.13.9"), Charset.forName("UTF-8"))
                d
              }
            }.flatMap { d =>
              Try {
                val targetPath = d.resolve("src").resolve("test").resolve("scala")
                val sourcePath = Paths.get(app.path.getAbsolutePath, "tests")
                Files.walkFileTree(sourcePath, new SimpleFileVisitor[Path] {
                  override def preVisitDirectory(dir: Path, attrs: BasicFileAttributes): FileVisitResult = {
                    Files.createDirectories(targetPath)
                    FileVisitResult.CONTINUE
                  }

                  override def visitFile(file: Path, attrs: BasicFileAttributes): FileVisitResult = {
                    Files.copy(file, targetPath.resolve(sourcePath.relativize(file)))
                    FileVisitResult.CONTINUE
                  }
                })
                d
              }
            }.flatMap { d =>
              Try(Process(Seq("sbt", "-Dsbt.log.noformat=true", "test"), d.toFile).!!)
            }.getOrElse("Test failed")
          })
        })
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

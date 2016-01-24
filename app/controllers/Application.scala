package controllers

import java.nio.charset.StandardCharsets
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
          p.success(Ok(createProjectAndTest(answer.prob)))
        })
        p.future
      })
  }

  def createProjectAndTest(solution: String): String = {
    import scala.collection.JavaConversions._
    Try {
      val buildSbt = List(
        """lazy val root = (project in file("."))""",
        """  .settings(""",
        """    scalaVersion := "2.11.7",""",
        """    libraryDependencies += "org.scalatest" %% "scalatest" % "2.2.6" % "test"""",
        """  )"""
      )
      val d = Files.createTempDirectory("test")
      Files.write(d.resolve("build.sbt"), buildSbt, StandardCharsets.UTF_8)

      val project = d.resolve("project")
      Files.createDirectory(project)

      Files.write(project.resolve("build.properties"), List("sbt.version=0.13.9"), StandardCharsets.UTF_8)

      val solutionTargetPath = d.resolve("src").resolve("main").resolve("scala")
      Files.createDirectories(solutionTargetPath)
      Files.write(solutionTargetPath.resolve("UserSolution.scala"), List("package tests", s"object SleepInSolution {$solution}"), StandardCharsets.UTF_8)

      val testTargetPath = d.resolve("src").resolve("test").resolve("scala")
      Files.createDirectories(testTargetPath)
      val testSourcePath = Paths.get(app.path.getAbsolutePath, "test", "tests")

      Files.walkFileTree(testSourcePath, new SimpleFileVisitor[Path] {
        override def visitFile(file: Path, attrs: BasicFileAttributes): FileVisitResult = {
          Files.copy(file, testTargetPath.resolve(testSourcePath.relativize(file)))
          FileVisitResult.CONTINUE
        }
      })
      val sbtCommands = Seq("sbt", "-Dsbt.log.noformat=true", "test")
      val output = new StringBuilder
      Process(sbtCommands, d.toFile).!(ProcessLogger(line => output append line append "\n"))
      output.toString()
    }.getOrElse("Test failed")
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

  lazy val sbtInstalled = sbt("sbtVersion").isSuccess // no exception, so sbt is in the PATH
}

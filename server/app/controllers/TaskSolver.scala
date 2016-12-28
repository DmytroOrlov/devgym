package controllers

import java.util.concurrent.TimeUnit
import java.util.{Date, UUID}
import javax.inject.Inject

import akka.actor.ActorSystem
import akka.stream.Materializer
import akka.stream.scaladsl.{Flow, Sink, Source}
import controllers.TaskSolver.{solution, _}
import dal.TaskDao
import models.{Language, Task}
import monifu.concurrent.Scheduler
import monifu.reactive.OverflowStrategy.DropOld
import monifu.reactive.channels.PublishChannel
import org.scalatest.Suite
import play.api.Logger
import play.api.cache.CacheApi
import play.api.data.Form
import play.api.data.Forms._
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.libs.json.{JsValue, Json}
import play.api.libs.streams.ActorFlow
import play.api.mvc.{Action, Controller, Request, WebSocket}
import service._
import service.reflection.{DynamicSuiteExecutor, RuntimeSuiteExecutor}
import shared.model.{Compiling, Event, Line}
import util.TryFuture

import scala.concurrent.Future
import scala.concurrent.duration._
import scala.language.postfixOps
import scala.util.control.NonFatal
import scala.util.{Failure, Success, Try}

class TaskSolver @Inject()(executor: RuntimeSuiteExecutor with DynamicSuiteExecutor,
                           dao: TaskDao, val messagesApi: MessagesApi, cache: CacheApi)
                          (implicit system: ActorSystem, s: Scheduler, mat: Materializer)
  extends Controller with I18nSupport with JSONFormats {

  val solutionForm = Form {
    mapping(
      solution -> nonEmptyText,
      year -> longNumber,
      lang -> nonEmptyText,
      timeuuid -> nonEmptyText
    )(SolutionForm.apply)(SolutionForm.unapply)
  }

  def getTask(year: Long, lang: String, timeuuid: UUID) = Action.async { implicit request: Request[_] =>
    def notFound = Redirect(routes.Application.index).flashing(flashToUser -> messagesApi("taskNotFound"))

    val task = TryFuture(getCachedTask(year, lang, timeuuid))
    task.map {
      case Some(t) => Ok(views.html.task(t.name, t.description,
        solutionForm.fill(SolutionForm(t.solutionTemplate, year, lang, timeuuid.toString))))
      case None => notFound
    }.recover { case NonFatal(e) => notFound }
  }

  def taskStream: WebSocket = WebSocket.accept { req =>
    val channel: PublishChannel[Event] = PublishChannel[Event](DropOld(20))
    val sink = Sink.foreach { clientInput: JsValue =>
      val prevTimestamp = (clientInput \ "prevTimestamp").as[Long]
      val currentTimestamp = (clientInput \ "currentTimestamp").as[Long]
      if (Duration(currentTimestamp - prevTimestamp, TimeUnit.MILLISECONDS) < 1.seconds) {
        channel.pushNext(Line("Too many requests per second from the same client. Slow down"))
        channel.pushComplete()
      } else {
        val solution = (clientInput \ "solution").as[String]
        val year = (clientInput \ "year").as[Long]
        val lang = (clientInput \ "lang").as[String]
        val timeuuid = (clientInput \ "timeuuid").as[String]

        getCachedTask(year, lang, UUID.fromString(timeuuid)).onComplete {
          case Success(Some(task)) =>
            channel.pushNext(service.testResult(Try(executor(solution, task.suite, task.solutionTrait)(s => channel.pushNext(Line(s))))))
            channel.pushComplete()
          case Success(None) =>
            channel.endWithError(new RuntimeException(s"Task is not available for a given solution: $solution"))
          case Failure(ex) =>
            channel.endWithError(ex)
        }
      }
    }
    Flow.fromSinkAndSource(sink, Source.fromPublisher(channel.map(Json.toJson(_)).timeout(10.seconds).toReactive))
  }

  private def getCachedTask(year: Long, lang: String, timeuuid: UUID): Future[Option[Task]] = {
    def getFromDb: Future[Option[Task]] = {
      Logger.debug(s"getting task from db: $year, $lang, $timeuuid")
      TryFuture(dao.getTask(new Date(year), Language.withName(lang), timeuuid))
    }

    val suiteKey = (year, lang, timeuuid).toString()
    val maybeTask = cache.get[Task](suiteKey)

    maybeTask match {
      case Some(_) => Future.successful(maybeTask)
      case None =>
        val f = getFromDb
        f.foreach {
          case Some(t) => cache.set(suiteKey, t, expiration)
          case None =>
        }
        f
    }
  }

  /**
    * it is an example of executing the test classes from the classPath. We only parse solution text coming from a user
    * Such approach is faster of course, than controllers.TaskSolver#taskStream().
    *
    * This approach can be used for predefined tests of DevGym platform to get better performance for demo tests.
    * Currently, we are not using this method and it should be removed from here to some snippet storage
    *
    * @return WebSocket
    */
  def runtimeTaskStream = WebSocket.accept { req =>
    ActorFlow.actorRef[JsValue, JsValue] { out =>
      SimpleWebSocketActor.props(out, (fromClient: JsValue) =>
        try {
          val suiteClass = "tasktest.SubArrayWithMaxSumTest"
          val solutionTrait = "tasktest.SubArrayWithMaxSumSolution"
          val solution = (fromClient \ "solution").as[String]
          Future.successful(ObservableRunner(executor(
            Class.forName(suiteClass).asInstanceOf[Class[Suite]],
            Class.forName(solutionTrait).asInstanceOf[Class[AnyRef]],
            solution), service.testResult))
        } catch {
          case NonFatal(e) => Future.failed(e)
        },
        Some(Compiling())
      )
    }
  }
}

case class SolutionForm(solution: String, year: Long, taskType: String, timeuuid: String)

object TaskSolver {
  val cannotCheckNow = "cannotCheckNow"
  val solution = "solution"
  val year = "year"
  val lang = "lang"
  val timeuuid = "timeuuid"

  val expiration = 60 seconds
}

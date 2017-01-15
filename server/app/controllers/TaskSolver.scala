package controllers

import java.util.concurrent.TimeUnit
import java.util.{Date, UUID}
import javax.inject.Inject

import akka.NotUsed
import akka.actor.ActorSystem
import akka.stream.Materializer
import akka.stream.scaladsl.{Flow, Sink, Source}
import controllers.TaskSolver.{solution, _}
import data.TaskDao
import models.{Language, Task}
import monix.execution.FutureUtils.extensions._
import monix.execution.Scheduler.Implicits.global
import monix.execution.cancelables.AssignableCancelable
import monix.reactive.{Observable, OverflowStrategy}
import org.scalatest.Suite
import play.api.Logger
import play.api.cache.CacheApi
import play.api.data.Form
import play.api.data.Forms._
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.{Action, Controller, WebSocket}
import service.reflection.{DynamicSuiteExecutor, RuntimeSuiteExecutor}
import shared.model.{Event, Line}

import scala.concurrent.duration.Duration._
import scala.concurrent.duration._
import scala.concurrent.{Future, Promise}
import scala.language.postfixOps
import scala.util.control.NonFatal
import scala.util.{Failure, Success}

class TaskSolver @Inject()(dynamicExecutor: DynamicSuiteExecutor, runtimeExecutor: RuntimeSuiteExecutor,
                           dao: TaskDao, val messagesApi: MessagesApi, cache: CacheApi)
                          (implicit system: ActorSystem, mat: Materializer)
  extends Controller with I18nSupport with JSONFormats {

  val solutionForm = Form {
    mapping(
      solution -> nonEmptyText,
      year -> longNumber,
      lang -> nonEmptyText,
      timeuuid -> nonEmptyText
    )(SolutionForm.apply)(SolutionForm.unapply)
  }

  def getTask(year: Long, lang: String, timeuuid: UUID) = Action.async { implicit request =>
    def notFound = Redirect(routes.Application.index).flashing(flashToUser -> messagesApi("taskNotFound"))

    val task = getCachedTask(year, lang, timeuuid)
    task.map {
      case Some(t) => Ok(views.html.task(t.name, t.description,
        solutionForm.fill(SolutionForm(t.solutionTemplate, year, lang, timeuuid.toString))))
      case None => notFound
    }.recover { case NonFatal(e) => notFound }
  }

  def taskStream = Action { req =>
    Ok.chunked(req.body.asJson.fold(Source.empty[JsValue]) { clientInput =>
      Source.fromPublisher(
        Observable.create[Event](OverflowStrategy.DropOld(20)) { downstream =>
        val cancelable = AssignableCancelable.single()
            val prevTimestamp = (clientInput \ "prevTimestamp").as[Long]
            val currentTimestamp = (clientInput \ "currentTimestamp").as[Long]
            if (Duration(currentTimestamp - prevTimestamp, TimeUnit.MILLISECONDS) < 1.seconds) {
              downstream.onNext(Line("Too many requests per second from the same client. Slow down"))
              downstream.onComplete()
            } else {
              val solution = (clientInput \ "solution").as[String]
              val year = (clientInput \ "year").as[Long]
              val lang = (clientInput \ "lang").as[String]
              val timeuuid = (clientInput \ "timeuuid").as[String]

              getCachedTask(year, lang, UUID.fromString(timeuuid)).onComplete {
                case Success(Some(task)) =>
                  val (checkNext, onBlockComplete) = service.testAsync { testResult =>
                    downstream.onNext(testResult)
                    downstream.onComplete()
                  }
                  val block: (String => Unit) => Unit = dynamicExecutor(solution, task.suite, task.solutionTrait)
                  cancelable := monix.eval.Task(block { next =>
                    downstream.onNext(Line(next))
                    checkNext(next)
                  }).runAsync(onBlockComplete)
                case Success(None) =>
                  downstream.onError(new RuntimeException(s"Task is not available for a given solution: $solution"))
                case Failure(ex) =>
                  downstream.onError(ex)
              }
            }
        cancelable
      }.map(Json.toJson(_)).toReactivePublisher)
    })
  }

  private def getCachedTask(year: Long, lang: String, timeuuid: UUID): Future[Option[Task]] = {
    def getFromDb: Future[Option[Task]] = {
      Logger.debug(s"getting task from db: $year, $lang, $timeuuid")
      dao.getTask(new Date(year), Language.withName(lang), timeuuid)
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
  def runtimeTaskStream = WebSocket.accept { _ =>
    val clientInputPromise = Promise[JsValue]()
    val channel: Observable[Event] =
      Observable.create[Event](OverflowStrategy.DropOld(20)) { downstream =>
        val cancelable = AssignableCancelable.single()
        clientInputPromise.future.timeout(1.second).onComplete {
            case Success(fromClient) =>
              val (checkNext, onBlockComplete) = service.testAsync { testResult =>
                downstream.onNext(testResult)
                downstream.onComplete()
              }
              cancelable := monix.eval.Task {
                val suiteClass = "tasktest.SubArrayWithMaxSumTest"
                val solutionTrait = "tasktest.SubArrayWithMaxSumSolution"
                val solution = (fromClient \ "solution").as[String]
                val block: (String => Unit) => Unit = runtimeExecutor(
                  Class.forName(suiteClass).asInstanceOf[Class[Suite]],
                  Class.forName(solutionTrait).asInstanceOf[Class[AnyRef]],
                  solution)
                block { next =>
                  downstream.onNext(Line(next))
                  checkNext(next)
                }
              }.runAsync(onBlockComplete)
            case Failure(ex) => downstream.onError(ex)
          }
        cancelable
      }
    val sink = Sink.foreach[JsValue](js => clientInputPromise.trySuccess(js))
    Flow.fromSinkAndSource(sink, Source.fromPublisher(channel.map(Json.toJson(_)).toReactivePublisher))
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

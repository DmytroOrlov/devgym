package controller

import java.util.Date

import client.SimpleWebSocketClient
import common.CodeEditor
import monifu.concurrent.Implicits.globalScheduler
import monifu.reactive.Observer
import monifu.reactive.OverflowStrategy.DropOld
import org.scalajs.dom
import shared.model._

import scala.concurrent.duration._
import scala.language.postfixOps
import scala.scalajs.js.Dynamic.{literal => obj}
import scala.scalajs.js.JSON

class AddTaskClient(editor: CodeEditor) {
  private val host = dom.window.location.host
  private val protocol = if (dom.document.location.protocol == "https:") "wss:" else "ws:"

  private val client = new SimpleWebSocketClient(
    url = s"$protocol//$host/getSolutionTemplate",
    DropOld(20),
    timeout = 15 minutes
    //, throttleDuration = Some(2 seconds)
  )
  private val source = client.collect { case AddTaskEvents(e) => e }

  object AddTaskEvents {
    def unapply(message: String): Option[Event] = {
      println(s"received new message = $message")
      val json = JSON.parse(message)

      def getTimestamp = json.timestamp.asInstanceOf[Number].longValue()

      json.name.asInstanceOf[String] match {
        case SolutionTemplate.name => Some(SolutionTemplate(
          code = json.code.asInstanceOf[String],
          timestamp = getTimestamp
        ))
        case "error" =>
          val errorType = json.`type`.asInstanceOf[String]
          val message = json.message.asInstanceOf[String]
          throw SimpleWebSocketClient.Exception(s"Server-side error thrown (${new Date(getTimestamp)}) - $errorType: $message")
        case _ => None
      }
    }
  }

  def subscribe(subscriber: Observer[Event]) = {
    source.subscribe(subscriber)
  }

  def sendSolutionCode(solution: String) = {
    client.sendEvent(obj("solution" -> solution))
  }

}

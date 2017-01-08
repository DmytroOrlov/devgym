package controller

import java.util.Date

import client.WebSocketClient
import common.CodeEditor
import monix.execution.Ack.Continue
import monix.execution.Scheduler.Implicits.global
import monix.execution.cancelables.BooleanCancelable
import monix.reactive.{Observable, OverflowStrategy}
import org.scalajs.jquery._
import shared.model.{Event, SolutionTemplate}

import scala.language.postfixOps
import scala.scalajs.js
import scala.scalajs.js.Dynamic.{literal => obj}
import scala.scalajs.js.{JSApp, JSON}

object AddTask extends JSApp {
  val solutionTemplateExample =
    """// if you want another person to implement 'apply' function,
      |// then just leave its body as empty
      |class SubArrayWithMaxSum {
      |  def apply(a: Array[Int]): Array[Int] = {
      |  // todo: implement ...
      |  }
      |}
      |""".stripMargin

  val suiteExample =
    """class MinDifferenceInArrayTest(solution: MinDifferenceInArraySolution)
      |  extends FlatSpec with Matchers {
      |
      |  it should "return 0 as min difference in array with non-unique numbers" in {
      |    solution.minDifference(Array(1, 2, 2, 2, 3, 12)) should be(0)
      |  }
      |
      |  it should "return non 0 as min difference in array with unique numbers" in {
      |    solution.minDifference(Array(1, 4, 10, 22, 43, 12)) should be(2)
      |  }
      |
      |  it should "return min difference in array with negative numbers" in {
      |    solution.minDifference(Array(1, 4, 10, 22, 43, -12)) should be(3)
      |  }
      |}
      |
      |trait MinDifferenceInArraySolution {
      |  def minDifference(a: Array[Int]): Int
      |}
      |""".stripMargin

  val buttonId = "submit"
  val solutionTemplateId = "solutionTemplate"
  val referenceSolutionId = "referenceSolution"
  val suiteId = "suite"

  val templateEditor = new CodeEditor(solutionTemplateId)
  val templateEditorExample = new CodeEditor("solutionTemplateExample", readOnly = true, solutionTemplateExample)
  val referenceEditor = new CodeEditor(referenceSolutionId)
  val suiteEditor = new CodeEditor(suiteId)
  val suiteEditorExample = new CodeEditor("suiteExample", readOnly = true, suiteExample)

  private val deriveSolutionTemplate = jQuery("#deriveSolutionTemplate")

  def main(): Unit = {
    def copyValues = {
      jQuery(s"#$solutionTemplateId").value(templateEditor.value)
      jQuery(s"#$referenceSolutionId").value(referenceEditor.value)
      jQuery(s"#$suiteId").value(suiteEditor.value)
    }

    jQuery(s"#$buttonId").click(copyValues)
    WebSocketClient.reconnecting(
      url = "getSolutionTemplate",
      messages = () => Observable.create[String](OverflowStrategy.DropOld(2)) { downstream =>
        val c = BooleanCancelable()
        referenceEditor.bindOnChangeHandler(() =>
          if (!c.isCanceled && deriveSolutionTemplate.is(":checked"))
            downstream.onNext(js.JSON.stringify(obj("solution" -> referenceEditor.value))))
        c
      })
      .collect { case AddTaskEvents(elem) => elem }
      .subscribe { elem =>
        templateEditor.setValue(elem)
        Continue
      }
  }
}

object AddTaskEvents {
  def unapply(message: String): Option[Event] = {
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
        throw new RuntimeException(s"Server-side error thrown (${new Date(getTimestamp)}) - $errorType: $message")
      case _ => None
    }
  }
}

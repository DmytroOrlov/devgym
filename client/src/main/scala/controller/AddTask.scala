package controller

import common.CodeEditor
import monifu.reactive.Ack.Continue
import monifu.reactive.Observer
import monifu.reactive.OverflowStrategy.DropOld
import monifu.reactive.channels.PublishChannel
import monifu.concurrent.Implicits.globalScheduler
import org.scalajs.jquery._
import shared.model.Event

import scala.concurrent.duration._
import scala.language.postfixOps
import scala.scalajs.js.JSApp

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

  private val refSolutionChannel = PublishChannel[String](DropOld(2)).throttleWithTimeout(2 seconds)
  refSolutionChannel.onSubscribe(new ReferenceSolutionObserver)

  def main(): Unit = {
    jQuery(s"#$buttonId").click(copyValues _)
    referenceEditor.bindOnChangeHandler(referenceSolutionOnChange)
  }

  private def copyValues() = {
    jQuery(s"#$solutionTemplateId").value(templateEditor.value)
    jQuery(s"#$referenceSolutionId").value(referenceEditor.value)
    jQuery(s"#$suiteId").value(suiteEditor.value)
  }

  private def referenceSolutionOnChange() = {
    println(referenceEditor.value)
    refSolutionChannel.pushNext(referenceEditor.value)
  }

  class ReferenceSolutionObserver extends Observer[String] {
    override def onNext(refSolution: String) = {
      val addTaskClient = new AddTaskClient(refSolution)
      addTaskClient.subscribe(new ReferenceTemplateObserver)
      Continue
    }

    override def onError(ex: Throwable) = {
      val m = s"${this.getClass.getName} $ex"
      System.err.println(m)
    }

    override def onComplete() = println("complete stream for ReferenceSolution")
  }

  class ReferenceTemplateObserver extends Observer[Event] {
    override def onNext(elem: Event) = {
      println(s"received elem = $elem")
      templateEditor.setValue(elem)
      Continue
    }

    override def onError(ex: Throwable) = {
      val m = s"${this.getClass.getName} $ex"
      System.err.println(m)
    }

    override def onComplete() = println("complete stream for ReferenceTemplate")
  }
}

package controller

import common.CodeEditor
import org.scalajs.jquery._

import scala.scalajs.js.JSApp

object AddTaskController extends JSApp {
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
  var templateEditor = new CodeEditor("solutionTemplate")
  var templateEditorExample = new CodeEditor("solutionTemplateExample", readOnly = true, solutionTemplateExample)
  var referenceEditor = new CodeEditor("referenceSolution")
  var suiteEditor = new CodeEditor("suite")
  var suiteEditorExample = new CodeEditor("suiteExample", readOnly = true, suiteExample)

  def main(): Unit = {
    jQuery(s"#$buttonId").click(submit _)

    def submit() = {
      jQuery("#solutionTemplate").value(templateEditor.value)
      jQuery("#referenceSolution").value(referenceEditor.value)
      jQuery("#suite").value(suiteEditor.value)
    }
  }
}

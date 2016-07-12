package controller

import common.CodeEditor
import org.scalajs.jquery._

import scala.scalajs.js.JSApp

object AddTaskController extends JSApp {
  val buttonId = "submit"
  var solutionTemplateEditor = new CodeEditor("solutionTemplate")
  var referenceSolutionEditor = new CodeEditor("referenceSolution")
  var suiteEditor = new CodeEditor("suite")

  def main(): Unit = {
    val submitButton = jQuery(s"#$buttonId")
    submitButton.click(submit _)

    def submit() = {
      jQuery("#solutionTemplate").value(solutionTemplateEditor.value)
      jQuery("#referenceSolution").value(referenceSolutionEditor.value)
      jQuery("#suite").value(suiteEditor.value)
    }
  }
}

package client

import org.scalajs.jquery.jQuery

import scala.scalajs.js
import scala.scalajs.js.Dynamic.{literal => obj, _}

object SubmitSolutionClient extends js.JSApp {
  def main(): Unit = {
    initSubmitter("submit", "report", "solution")
  }

  def initSubmitter(buttonId: String, reportId: String, solutionId: String) = {
    def submit() = {
      val report = jQuery(s"#$reportId")
      report.empty()

      def reportData(data: String) = report.html(data)

      val solution = jQuery(s"#$solutionId").`val`()
      global.jsRoutes.controllers.TaskSolver.postSolutionAjax(solution).ajax(obj(
        "success" -> reportData _
      ))
    }

    jQuery(s"#$buttonId").click(submit _)
  }
}

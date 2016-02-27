package shared.view

object SuiteReportUtil {
  //scalatest library specific characters for colors
  val green = "\u001B[32m"
  val close = "\u001B[0m"
  val red = "\u001B[31m"

  val reflectionWrapperPattern = """__wrapper([\w:\n\$]*)"""
  val compilationFailed = "reflective compilation has failed:"

  def enhanceReport(report: Option[String]) = {
    report.map { r =>
      s"<p id='errorReport'>${
        removeToolboxText(replaceMarkers(r, "</span><br/>"))
      }</p>"
    }.getOrElse("")
  }

  def replaceMarkers(report: String, lineEnd: String = "</span>") = {
    report
      .replace(close, lineEnd)
      .replace(green, """<span class="green">""")
      .replace(red, """<span class="red">""")
  }

  def removeToolboxText(s: String) = {
    s.replaceFirst(reflectionWrapperPattern, "").replaceFirst(compilationFailed, "")
  }
}

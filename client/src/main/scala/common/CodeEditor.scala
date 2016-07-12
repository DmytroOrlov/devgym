package common

import scala.scalajs.js

class CodeEditor(editorId: String) {
  val ace = js.Dynamic.global.ace
  val editor = ace.edit(editorId)
  editor.setTheme("ace/theme/tomorrow")
  editor.getSession().setMode("ace/mode/scala")

  def value = editor.getValue().asInstanceOf[String]
}

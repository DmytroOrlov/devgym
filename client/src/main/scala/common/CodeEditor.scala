package common

import scala.scalajs.js

class CodeEditor(editorId: String, readOnly: Boolean = false, initialText: String = "") {
  val ace = js.Dynamic.global.ace
  val editor = ace.edit(editorId)
  editor.setTheme("ace/theme/tomorrow")
  editor.getSession().setMode("ace/mode/scala")
  editor.setReadOnly(readOnly)
  editor.insert(initialText)

  def value = editor.getValue().asInstanceOf[String]
}

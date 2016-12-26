package common

import shared.model.{Event, SolutionTemplate}

import scala.scalajs.js
import scala.scalajs.js.Dynamic
import scala.scalajs.js.Dynamic.{literal => obj}

class CodeEditor(editorId: String, readOnly: Boolean = false, initialText: String = "") {

  val ace: Dynamic = js.Dynamic.global.ace
  ace.require("ace/ext/language_tools")

  val editor: Dynamic = ace.edit(editorId)
  editor.setTheme("ace/theme/tomorrow")
  editor.getSession().setMode("ace/mode/scala")
  editor.setOptions(obj(
  enableBasicAutocompletion = true,
  enableLiveAutocompletion = true
  ))
  editor.setReadOnly(readOnly)
  editor.insert(initialText)

  def value = editor.getValue().asInstanceOf[String]

  def bindOnChangeHandler(onChangeHandler: () => Unit) = {
    editor.on("change", onChangeHandler)
  }

  def bindShortcut(commandName: String, winKeys: String, macKeys: String, handler: Any => Unit): Unit = {
    editor.commands.addCommand(obj(
    name = commandName,
    bindKey = obj(win = winKeys, mac = macKeys),
    exec = handler
    ))
  }

  def insert(elem: Event) = elem match {
    case s: SolutionTemplate => editor.insert(s.code)
    case _ => println(s"Unsupported event $elem")
  }
}

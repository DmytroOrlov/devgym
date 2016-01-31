package client

import shared.Shared

import scala.scalajs.js
import scala.scalajs.js.Dynamic._

object SampleClient extends js.JSApp {
  def log(message: String) = {
    val canLog = !js.isUndefined(global.console) &&
      !js.isUndefined(global.console.log)
    if (canLog) global.console.log(message)
  }

  def main(): Unit = {
    val s = Shared("123")
    log(s.value)
  }
}

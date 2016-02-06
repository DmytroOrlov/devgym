package service

import scala.util.Try



/**
 * Runs test suite of scalatest library using the 'execute' method
 */
class ScalaTestRunner extends ScalaRuntimeRunner with ScalaDynamicRunner with ScalaDynamicNoTraitRunner {
  override def failurePrefix(s: String): String = s"Test $failedInRuntime with error:\n"
}

object ScalaTestRunner {

  case class SuiteException(msg: String) extends RuntimeException(msg)

}

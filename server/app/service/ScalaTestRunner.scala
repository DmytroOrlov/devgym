package service

/**
 * Runs test suite of scalatest library using the 'execute' method
 */
class ScalaTestRunner extends ScalaRuntimeRunner with ScalaDynamicRunner with ScalaDynamicNoTraitRunner {
  val validate = (s: String) => !s.contains(failed)
}

object ScalaTestRunner {

  case class SuiteException(msg: String) extends RuntimeException(msg)

}

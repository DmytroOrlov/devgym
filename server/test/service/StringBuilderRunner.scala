package service

import shared.model.TestResult

import scala.concurrent.ExecutionContext
import scala.util.{Success, Try}

object StringBuilderRunner {
  def apply(block: (String => Unit) => Unit,
            testResult: Try[String] => Option[TestResult] = { _ => None })
           (implicit ec: ExecutionContext): String = {

    val sb = new StringBuilder
    block(s => sb.append(s))

    testResult(Success(sb.toString()))
      .map(_.testStatus.toString)
      .foreach(sb.append)

    sb.toString()
  }
}

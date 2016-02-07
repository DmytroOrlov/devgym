package service

import monifu.concurrent.Implicits.globalScheduler
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.time.Span._
import org.scalatest.{FlatSpec, Matchers, Suite}

import scala.concurrent.duration._

class ScalaTestRunnerTest extends FlatSpec with Matchers with ScalaTestCorrectSolution with ScalaFutures {
  behavior of "ScalaTestRunner"
  val incorrectSolution = "class A { def sleepIn(weekday: Boolean, vacation: Boolean): Boolean = {weekday || vacation}}"

  implicit val c = PatienceConfig(10.seconds)

  it should "return value when correct solution is provided" in {
    getReport(correctSolution)._2.futureValue
  }

  it should "return value when compilable solution is provided" in {
    getReport(incorrectSolution)._2.futureValue
  }

  it should "return failure when check compilable but wrong solution" in {
    getReport(incorrectSolution, checked = true)._2.failed.futureValue
  }

  it should "return failure when solution is not compilable" in {
    getReport("/")._2.failed.futureValue
  }

  private val r = new ScalaTestRunner()

  def getReport(solution: String, checked: Boolean = false) = {
    val unchecked = r.apply(
      Class.forName("service.SleepInTest").asInstanceOf[Class[Suite]],
      Class.forName("service.SleepInSolution").asInstanceOf[Class[AnyRef]]
    )(solution)
    if (checked) r.check(unchecked)
    else unchecked
  }
}

trait ScalaTestCorrectSolution {
  val correctSolution = "class A { def sleepIn(weekday: Boolean, vacation: Boolean): Boolean = {!weekday || vacation}}"
}

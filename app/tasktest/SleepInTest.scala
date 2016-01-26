package tasktest

import org.scalatest.{FlatSpec, Matchers}


//TODO: to be removed from the project
class SleepInTest(solution: SleepInSolution) extends FlatSpec with Matchers {
  behavior of "SleepIn"

  it should "sleepIn when it is not a weekday and it is not a vacation" in {
    solution.sleepIn(false, false) shouldBe true
  }

  it should "not sleepIn when it is a weekday and it is not a vacation" in {
    solution.sleepIn(true, false) shouldBe false
  }

  it should "sleepIn when it is not a weekday and it is a vacation" in {
    solution.sleepIn(false, true) shouldBe true
  }
}

trait SleepInSolution {
  def sleepIn(weekday: Boolean, vacation: Boolean): Boolean
}

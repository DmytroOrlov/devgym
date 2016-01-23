package tests

import org.scalatest.{FlatSpec, Matchers}

class SleepInTest extends FlatSpec with Matchers {
  behavior of "SleepIn"

  it should "sleepIn when it is not a weekday and it is not a vacation" in {
    UserSolution.sleepIn(false, false) shouldBe true
  }

  it should "not sleepIn when it is a weekday and it is not a vacation" in {
    UserSolution.sleepIn(true, false) shouldBe false

  }

  it should "sleepIn when it is not a weekday and it is a vacation" in {
    UserSolution.sleepIn(false, true) shouldBe true
  }
}

object UserSolution {
  def sleepIn(weekday: Boolean, vacation: Boolean): Boolean = {
    !weekday || vacation
  }
}

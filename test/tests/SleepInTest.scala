package tests

import org.scalatest.{FlatSpec, Matchers}

class SleepInTest extends FlatSpec with Matchers {
  behavior of "SleepIn"

  it should "sleepIn when it is not a weekday and it is not a vacation" in {
    SleepInSolution.sleepIn(false, false) shouldBe true
  }

  it should "not sleepIn when it is a weekday and it is not a vacation" in {
    SleepInSolution.sleepIn(true, false) shouldBe false
  }

  it should "sleepIn when it is not a weekday and it is a vacation" in {
    SleepInSolution.sleepIn(false, true) shouldBe true
  }
}
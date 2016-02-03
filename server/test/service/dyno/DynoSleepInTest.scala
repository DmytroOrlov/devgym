package service.dyno

import org.scalatest.{FlatSpec, Matchers}

import scala.language.reflectiveCalls

class DynoSleepInTest[A <: {def sleepIn(weekday : Boolean, vacation : Boolean) : Boolean}](solution: A) extends FlatSpec with Matchers {
  behavior of "SleepIn: this test to be run from Runner test"

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

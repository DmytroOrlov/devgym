package tests

object SleepInSolution {
    def sleepIn(weekday: Boolean, vacation: Boolean): Boolean = {
      !weekday || vacation
    }
}

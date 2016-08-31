package tag

import org.scalatest.Tag

/**
  * to exclude run sbt "testOnly -- -l RequireDB -l PerfTests"
  * or sbt unit:test
  */
object RequireDB extends Tag("RequireDB")

object PerfTests extends Tag("PerfTests")

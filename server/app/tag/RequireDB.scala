package tag

import org.scalatest.Tag

/**
 * to exclude run sbt "testOnly -- -l RequireDB"
 * or sbt unit:test
 */
object RequireDB extends Tag("RequireDB")

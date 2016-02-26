package shared

sealed trait Event {
  def event: String

  def timestamp: Long
}

case class Line(value: String, timestamp: Long = System.currentTimeMillis()) extends Event {
  val event = "line"
}

object Line {
  val reportComplete = "DevGym_Done"
}

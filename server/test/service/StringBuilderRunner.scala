package service

object StringBuilderRunner {
  def apply(block: (String => Unit) => Unit): String = {
    val sb = new StringBuilder
    block(s => sb.append(s))
    sb.toString()
  }
}

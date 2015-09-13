package integration.util

object Text {
  def randomStr(size: Int = 10) = scala.util.Random.alphanumeric.take(size).mkString
}

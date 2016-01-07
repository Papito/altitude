package altitude.exceptions

case class IllegalOperationException(msg: String) extends IllegalArgumentException(msg)

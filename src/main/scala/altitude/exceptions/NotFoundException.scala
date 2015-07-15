package altitude.exceptions

class NotFoundException(msg: String) extends Exception(msg) {
  def this() = this("")
}


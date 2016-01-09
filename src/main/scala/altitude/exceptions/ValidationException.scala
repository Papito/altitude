package altitude.exceptions

import scala.collection.mutable
import altitude.{Const => C}

case class ValidationException(msg: String = "") extends Exception {
  val errors: mutable.Map[String, String] = mutable.Map()

  def message = {
    errors.isEmpty match {
      case true => if (msg.isEmpty) C.MSG("err.validation_error") else msg
      case false => {
        val fieldList = errors.keys.mkString(", ")
        C.MSG("err.validation_errors").format(fieldList)
      }
    }
  }
}

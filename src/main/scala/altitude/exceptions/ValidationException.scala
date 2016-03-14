package altitude.exceptions

import altitude.{Const => C}

import scala.collection.mutable

case class ValidationException(msg: String = "") extends Exception {
  val errors: mutable.Map[String, String] = mutable.Map()

  def message = {
    errors.isEmpty match {
      case true => if (msg.isEmpty) C("msg.err.validation_error") else msg
      case false => {
        val fieldList = errors.keys.mkString(", ")
        C("msg.err.validation_errors").format(fieldList)
      }
    }
  }
}

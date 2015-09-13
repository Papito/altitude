package altitude.exceptions

import scala.collection.mutable
import altitude.{Const => C}

case class ValidationException() extends Exception {
  val errors: mutable.Map[String, String] = mutable.Map()

  def message = {
    val fieldList = errors.keys.mkString(", ")
    C.MSG("err.validation_errors").format(fieldList)
  }
}

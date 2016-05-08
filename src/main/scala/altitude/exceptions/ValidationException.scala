package altitude.exceptions

import altitude.{Const => C}

import scala.collection.mutable

case class ValidationException(message: String = "") extends Exception {
  val errors: mutable.Map[String, String] = mutable.Map()
}

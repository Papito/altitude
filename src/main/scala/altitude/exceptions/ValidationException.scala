package altitude.exceptions

import scala.collection.mutable

class ValidationException extends Exception {
  val errors: mutable.Map[String, String] = mutable.Map()
}

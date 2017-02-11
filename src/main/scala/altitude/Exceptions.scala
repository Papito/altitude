package altitude

import altitude.models.Asset
import play.api.libs.json.JsObject

import scala.collection.mutable

case class ValidationException(message: String = "") extends Exception {
  val errors: mutable.Map[String, String] = mutable.Map()
  final def isEmpty = message.isEmpty && errors.isEmpty
  final def nonEmpty = !isEmpty
}

// All-purpose event to get out of loops with user interrupts or conditionals
case class AllDone() extends Exception

case class DuplicateException(objJson: JsObject, duplicateOf: JsObject) extends Exception

case class ConstraintException(msg: String) extends Exception(msg)

case class FieldValueException() extends IllegalArgumentException

case class FormatException(asset: Asset) extends RuntimeException()

case class IllegalOperationException(msg: String) extends IllegalArgumentException(msg)

case class MetadataExtractorException(asset: Asset, ex: Throwable) extends Exception(ex)

case class NotFoundException(msg: String) extends Exception(msg)

class StorageException(msg: String) extends Exception(msg)

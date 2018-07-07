package software.altitude.core

import play.api.libs.json.JsObject
import software.altitude.core.models.Asset

import scala.collection.mutable

case class ValidationException(message: String = "") extends Exception {
  val errors: mutable.Map[String, String] = mutable.Map()
  final def isEmpty: Boolean = message.isEmpty && errors.isEmpty
  final def nonEmpty: Boolean = !isEmpty

  def trigger(): Unit = {
    if (nonEmpty) {
      throw this
    }
  }
}

// All-purpose event to get out of loops with user interrupts or conditionals
case class AllDone() extends Exception

case class DuplicateException(existingAssetId: String) extends Exception

case class ConstraintException(msg: String) extends Exception(msg)

case class FieldValueException() extends IllegalArgumentException

case class FormatException(asset: Asset) extends RuntimeException()

case class IllegalOperationException(msg: String) extends IllegalArgumentException(msg)

case class MetadataExtractorException(asset: Asset, ex: Throwable) extends Exception(ex)

case class NotFoundException(msg: String) extends Exception(msg)

case class StorageException(msg: String) extends Exception(msg)

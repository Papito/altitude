package software.altitude.core

import scala.collection.mutable

import software.altitude.core.models.Asset

case class ValidationException(message: String = "") extends Exception {
  final def isEmpty: Boolean = message.isEmpty && errors.isEmpty
  final def nonEmpty: Boolean = !isEmpty

  val errors: mutable.Map[String, String] = mutable.Map()

  def trigger(): Unit = {
    if (nonEmpty) {
      throw this
    }
  }
}

// All-purpose event to get out of loops and sticky situations
case class AllDone(success: Boolean) extends Exception

case class DuplicateException(message: Option[String] = None) extends Exception

case class ConstraintException(msg: String) extends Exception(msg)

case class UnsupportedMediaTypeException(asset: Asset) extends Exception()

case class IllegalOperationException(msg: String) extends IllegalArgumentException(msg)

case class MetadataExtractorException(asset: Asset, ex: Throwable) extends Exception(ex)

case class NotFoundException(msg: String) extends Exception(msg)

case class StorageException(msg: String) extends Exception(msg)

case class SamePersonDetectedTwiceException(msg: String) extends Exception(msg)

package models

abstract class AbstractMetaField[T](val name: String, val isMulti: Boolean = false) {
  require(name != null)
  require(name.nonEmpty)

  def checkValue(value: T): Unit = Unit // everything allowed by default
}

package models

abstract class AbstractMetaValue[T](val value: T, private val metaField: AbstractMetaField[T]) {
  metaField.checkValue(value)
}

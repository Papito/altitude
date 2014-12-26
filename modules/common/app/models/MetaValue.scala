package models

abstract class MetaValue[T](val value: T, val metaField: AbstractMetaField[T]) {
  protected def checkValue(value: T): Unit
}

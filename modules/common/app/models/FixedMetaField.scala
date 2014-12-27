package models

import exceptions.FieldValueException

class FixedMetaField[T](name: String, isMulti: Boolean=false, val allowed: Set[T]=null)
  extends MetaField[T](name, isMulti=isMulti) {
  require(allowed.nonEmpty)

  override def checkValue(value: T): Unit = {
    if (!allowed.contains(value)) {
      throw new FieldValueException
    }
  }
}

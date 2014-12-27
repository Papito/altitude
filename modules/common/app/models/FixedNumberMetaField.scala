package models

import exceptions.FieldValueException

class FixedNumberMetaField(name: String, isMulti: Boolean=false, val allowed: Set[Long]=null)
  extends NumberMetaField(name, isMulti=isMulti)
  with FixedField[Long] {
  require(allowed.nonEmpty)

  override def checkValue(value: Long): Unit = {
    if (!allowed.contains(value)) {
      throw new FieldValueException
    }
  }
}

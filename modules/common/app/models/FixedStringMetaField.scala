package models

import exceptions.FieldValueException

class FixedStringMetaField(name: String, isMulti: Boolean=false, val allowed: Set[String]=null)
  extends StringMetaField(name, isMulti=isMulti)
  with FixedField[String] {
  require(allowed.nonEmpty)

  override def checkValue(value: String): Unit = {
    if (!allowed.contains(value)) {
      throw new FieldValueException
    }
  }
}

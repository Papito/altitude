package models

class StringMetaValue(value: String, field: StringMetaField)
  extends MetaValue[String](value, field) {

  override def checkValue(value: String): Unit = Unit
}

package models

class StringMetaField(name: String, isMulti: Boolean = false)
  extends AbstractMetaField[String](name, isMulti = isMulti) {
}

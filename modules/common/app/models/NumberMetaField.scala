package models

class NumberMetaField(name: String, isMulti: Boolean = false)
  extends AbstractMetaField[Long](name, isMulti = isMulti) {
}

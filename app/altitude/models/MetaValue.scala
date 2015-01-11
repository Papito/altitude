package altitude.models

class MetaValue[T](val value: T, private val metaField: MetaField[T]) {
  metaField.checkValue(value)
}

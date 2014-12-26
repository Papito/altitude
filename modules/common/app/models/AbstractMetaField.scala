package models

abstract class AbstractMetaField[T](val name: String, val isMulti: Boolean = false) {
  require(name != Nil)
  require(!name.trim.isEmpty)
  def check(value: MetaValue[T])
}

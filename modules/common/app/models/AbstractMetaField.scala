package models

abstract class AbstractMetaField(val name: String, val isMulti: Boolean = false) {
  require(name != Nil)
  require(!name.trim.isEmpty)
  type T
  def check(value: MetaValue[T])
}

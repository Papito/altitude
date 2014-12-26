package models

class StringMetaField(name: String, isMulti: Boolean = false)
  extends AbstractMetaField(name, isMulti = isMulti) {

  type T = String
  def check(value: MetaValue[String]): Unit = Unit // we like ALL strings
}

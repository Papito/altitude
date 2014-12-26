package models

class StringMetaField(name: String, isMulti: Boolean = false)
  extends AbstractMetaField[String](name, isMulti = isMulti) {

  def check(value: MetaValue[String]): Unit = Unit // we like ALL strings
}

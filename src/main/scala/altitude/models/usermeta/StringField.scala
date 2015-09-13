package altitude.models.usermeta

case class StringField(name: String,
                       allowsMulti: Boolean,
                       restrictedValueList: List[String]) extends AbstractField {
  override val fieldType = FieldType.STRING
  override val maxLength = 255
}


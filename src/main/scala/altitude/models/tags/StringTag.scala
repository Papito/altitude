package altitude.models.tags

case class StringTag(name: String,
                     allowsMulti: Boolean,
                     restrictedValueList: List[String]) extends AbstractTag {
  override val tagType = TagType.STRING
  override val maxLength = 255
}


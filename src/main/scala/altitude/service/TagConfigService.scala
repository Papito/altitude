package altitude.service

import altitude.Altitude
import altitude.models.tags.{StringTag, AbstractTag}

class TagConfigService(val app: Altitude) {
  private val EMPTY_LIST = List[String]()

  def getAll: List[AbstractTag] = {
    val keywordField = StringTag(
      id = Some("1"),
      name = "Keywords",
      allowsMulti = true,
      restrictedValueList = EMPTY_LIST)

    List(keywordField)
  }
}

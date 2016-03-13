package altitude.service

import altitude.Altitude
import altitude.models.tags.{AbstractTag, StringTag}

object TagConfigService {
  val KEYWORDS_TAG_ID = "1" // system tag
}

class TagConfigService(val app: Altitude) {
  private val EMPTY_LIST = List[String]()

  def getAll: List[AbstractTag] = {
    val keywordField = StringTag(
      id = Some(TagConfigService.KEYWORDS_TAG_ID),
      name = "Keywords",
      allowsMulti = true,
      restrictedValueList = EMPTY_LIST)

    List(keywordField)
  }
}

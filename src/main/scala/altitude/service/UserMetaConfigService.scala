package altitude.service

import altitude.models.usermeta.{StringField, AbstractField}

class UserMetaConfigService {
  private val EMPTY_LIST = List[String]()

  def getAll: List[AbstractField] = {
    val keywordField = StringField(
      name = "keywords",
      allowsMulti = true,
      restrictedValueList = EMPTY_LIST)

    List(keywordField)
  }
}

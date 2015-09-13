package altitude.service

import altitude.Altitude
import altitude.models.usermeta.{StringField, AbstractField}

class UserMetaConfigService(val app: Altitude) {
  private val EMPTY_LIST = List[String]()

  def getAll: List[AbstractField] = {
    val keywordField = StringField(
      name = "Keywords",
      allowsMulti = true,
      restrictedValueList = EMPTY_LIST)

    List(keywordField)
  }
}

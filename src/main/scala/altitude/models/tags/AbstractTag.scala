package altitude.models.tags

import altitude.{Const => C}

/*
abstract class AbstractTag extends BaseModel {
  val name: String
  val tagType: TagType.Value
  val maxLength: Int
  val allowsMulti: Boolean
  val restrictedValueList: List[String]

  def toJson: JsObject = Json.obj(
    C.Tag.NAME -> name,
    C.Tag.TYPE -> tagType.toString,
    C.Tag.MAX_LENGTH -> maxLength,
    C.Tag.ALLOWS_MULTI -> allowsMulti,
    C.Tag.RESTRICTED_VALUE_LIST -> JsArray( restrictedValueList.map(JsString) )
  ) ++ coreJsonAttrs
}
*/

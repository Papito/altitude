package altitude.models.usermeta

import play.api.libs.json.{JsString, JsArray, Json, JsObject}
import altitude.{Const => C}

abstract class AbstractField {
  val name: String
  val fieldType: FieldType.Value
  val maxLength: Int
  val allowsMulti: Boolean
  val restrictedValueList: List[String]

  lazy val toJson: JsObject = Json.obj(
    C.UserMetaField.NAME -> name,
    C.UserMetaField.TYPE -> fieldType.toString,
    C.UserMetaField.MAX_LENGTH -> maxLength,
    C.UserMetaField.ALLOWS_MULTI -> allowsMulti,
    C.UserMetaField.RESTRICTED_VALUE_LIST -> JsArray( restrictedValueList.map(JsString) )
  )
}

package altitude.models

import play.api.libs.json.{Json, JsValue}
import altitude.{Const => C}

import scala.language.implicitConversions

object MetadataField {
  implicit def fromJson(json: JsValue): MetadataField = MetadataField(
      id = (json \ C("Base.ID")).asOpt[String],
      userId = (json \ C("Base.USER_ID")).as[String],
      name = (json \ C("MetadataField.NAME")).as[String],
      fieldType = (json \ C("MetadataField.FIELD_TYPE")).as[String],
      isFixedList = (json \ C("MetadataField.IS_FIXED_LIST")).as[Boolean],
      maxLength = (json \ C("MetadataField.MAX_LENGTH")).asOpt[Int]
    ).withCoreAttr(json)
}

case class MetadataField(
                  id: Option[String] = None,
                  userId: String,
                  name: String,
                  fieldType: String,
                  isFixedList: Boolean = false,
                  maxLength: Option[Int] = None) extends BaseModel {

  val nameLowercase = name.toLowerCase

  override def toJson = Json.obj(
      C("Base.USER_ID") -> userId,
      C("MetadataField.NAME") -> name,
      C("MetadataField.NAME_LC") -> nameLowercase,
      C("MetadataField.FIELD_TYPE") -> fieldType,
      C("MetadataField.IS_FIXED_LIST") -> isFixedList,
      C("MetadataField.MAX_LENGTH") -> maxLength
    ) ++ coreJsonAttrs

}

package altitude.models

import altitude.{Const => C}
import play.api.libs.json._

import scala.language.implicitConversions

object FieldType extends Enumeration {
  val STRING = Value("STRING")
  val NUMBER = Value("NUMBER")
  val FLAG = Value("FLAG")
}

object UserMetadataField {
  implicit def fromJson(json: JsValue): UserMetadataField =
    UserMetadataField(
      id = (json \ C.Base.ID).asOpt[String],
      userId = (json \ C.Base.USER_ID).as[String],
      name = (json \ C.MetadataField.NAME).as[String],
      fieldType = (json \ C.MetadataField.FIELD_TYPE).as[String],
      constraintList = (json \ C.MetadataField.CONSTRAINT_LIST).asOpt[List[String]],
      maxLength = (json \ C.MetadataField.MAX_LENGTH).asOpt[Int]
    ).withCoreAttr(json)
}

case class UserMetadataField(
                  id: Option[String] = None,
                  userId: String,
                  name: String,
                  fieldType: String,
                  constraintList: Option[List[String]] = None,
                  maxLength: Option[Int] = None) extends BaseModel {

  val nameLowercase = name.toLowerCase

  override def toJson = Json.obj(
      C.Base.USER_ID -> userId,
      C.MetadataField.NAME -> name,
      C.MetadataField.NAME_LC -> nameLowercase,
      C.MetadataField.FIELD_TYPE -> fieldType,
      C.MetadataField.CONSTRAINT_LIST -> {
        if (constraintList.isEmpty) JsNull else Json.toJson(constraintList.get)
      },
      C.MetadataField.MAX_LENGTH -> maxLength
    ) ++ coreJsonAttrs

}

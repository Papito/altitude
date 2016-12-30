package altitude.models

import altitude.{Const => C}
import play.api.libs.json._

import scala.language.implicitConversions

object FieldType extends Enumeration {
  val TEXT = Value("TEXT")
  val NUMBER = Value("NUMBER")
  val BOOL = Value("BOOL")
  val DATETIME = Value("DATETIME")
}

object MetadataField {
  implicit def fromJson(json: JsValue): MetadataField =
    MetadataField(
      id = (json \ C.Base.ID).asOpt[String],
      name = (json \ C.MetadataField.NAME).as[String],
      fieldType = (json \ C.MetadataField.FIELD_TYPE).as[String]
    ).withCoreAttr(json)
}

case class MetadataField(
                  id: Option[String] = None,
                  name: String,
                  fieldType: String) extends BaseModel {

  val nameLowercase = name.toLowerCase

  override def toJson = Json.obj(
      C.MetadataField.NAME -> name,
      C.MetadataField.NAME_LC -> nameLowercase,
      C.MetadataField.FIELD_TYPE -> fieldType
    ) ++ coreJsonAttrs

}

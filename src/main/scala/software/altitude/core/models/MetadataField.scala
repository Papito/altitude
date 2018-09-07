package software.altitude.core.models

import play.api.libs.json._
import software.altitude.core.{Const => C}

import scala.language.implicitConversions

object FieldType extends Enumeration {
  val KEYWORD: Value = Value("KEYWORD")
  val TEXT: Value = Value("TEXT")
  val NUMBER: Value = Value("NUMBER")
  val BOOL: Value = Value("BOOLEAN")
  val DATETIME: Value = Value("DATETIME")
}

object MetadataField {
  implicit def fromJson(json: JsValue): MetadataField =
    MetadataField(
      id = (json \ C.Base.ID).asOpt[String],
      name = (json \ C.MetadataField.NAME).as[String],
      fieldType = FieldType.withName((json \ C.MetadataField.FIELD_TYPE).as[String])
    ).withCoreAttr(json)
}

case class MetadataField(id: Option[String] = None,
                         name: String,
                         fieldType: FieldType.Value) extends BaseModel {
  val nameLowercase: String = name.toLowerCase

  override def toJson: JsObject = Json.obj(
      C.MetadataField.NAME -> name,
      C.MetadataField.NAME_LC -> nameLowercase,
      C.MetadataField.FIELD_TYPE -> fieldType.toString
    ) ++ coreJsonAttrs

}

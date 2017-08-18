package software.altitude.core.models

import software.altitude.core.{Const => C}
import play.api.libs.json._

import scala.language.implicitConversions

object FieldType extends Enumeration {
  val KEYWORD = Value("KEYWORD")
  val TEXT = Value("TEXT")
  val NUMBER = Value("NUMBER")
  val BOOL = Value("BOOLEAN")
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
  val nameLowercase = name.toLowerCase

  override def toJson = Json.obj(
      C.MetadataField.NAME -> name,
      C.MetadataField.NAME_LC -> nameLowercase,
      C.MetadataField.FIELD_TYPE -> fieldType.toString
    ) ++ coreJsonAttrs

}

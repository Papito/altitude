package altitude.models

import altitude.{Const => C}
import play.api.libs.json.{JsValue, Json}
import scala.language.implicitConversions

object MetadataValue {
  implicit def fromJson(json: JsValue): MetadataValue =
    MetadataValue(
      id = (json \ C.Base.ID).asOpt[String],
      assetId = (json \ C.MetadataValue.ASSET_ID).as[String],
      fieldId = (json \ C.MetadataValue.FIELD_ID).as[String],
      value = (json \ C.MetadataValue.FIELD_VALUE).as[String]
    ).withCoreAttr(json)
}

case class MetadataValue(id: Option[String] = None,
                         assetId: String,
                         fieldId: String,
                         value: String) extends BaseModel {

  val valueLowerCase = value.toLowerCase

  override def toJson = Json.obj(
    C.MetadataValue.ASSET_ID -> assetId,
    C.MetadataValue.FIELD_ID -> fieldId,
    C.MetadataValue.FIELD_VALUE -> value,
    C.MetadataValue.FIELD_VALUE_LC -> valueLowerCase
  ) ++ coreJsonAttrs
}
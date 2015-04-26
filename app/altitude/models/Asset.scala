package altitude.models

import play.api.libs.json._
import altitude.{Const => C}

import scala.language.implicitConversions

object Asset {
  implicit def fromJson(json: JsValue): Asset = Asset(
      id = (json \ C.Asset.ID).asOpt[String],
      mediaType = json \ C.Asset.MEDIA_TYPE,
      path = (json \ C.Asset.PATH).as[String],
      md5 = (json \ C.Asset.MD5).as[String],
      metadata = json \ C.Asset.METADATA
    ).withCoreAttr(json)
  }

case class Asset(id: Option[String] = None,
                 mediaType: MediaType,
                 path: String,
                 md5: String,
                 metadata: JsValue = JsNull) extends BaseModel {

  override def toJson = Json.obj(
    C.Asset.PATH -> path,
    C.Asset.MD5 -> md5,
    C.Asset.MEDIA_TYPE -> (mediaType: JsValue),
    C.Asset.METADATA -> metadata) ++ coreJsonAttrs
}
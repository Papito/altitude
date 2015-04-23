package altitude.models

import org.joda.time.{DateTime}
import play.api.libs.json._
import altitude.{Const => C}

import scala.language.implicitConversions

object Asset {
  implicit def fromJson(json: JsValue): Asset = Asset(
      id = (json \ C.Asset.ID).asOpt[String],
      mediaType = json \ C.Asset.MEDIA_TYPE,
      locations = (json \ C.Asset.LOCATIONS).as[List[JsValue]].map(AssetLocation.fromJson),
      metadata = json \ C.Asset.METADATA
    ).withCoreAttr(json)
  }

case class Asset(id: Option[String] = None,
                 mediaType: MediaType,
                 locations: List[AssetLocation],
                 metadata: JsValue = JsNull) extends BaseModel {

  override def toJson = Json.obj(
      C.Asset.LOCATIONS -> locations.map(_.toJson),
      C.Asset.MEDIA_TYPE -> (mediaType: JsValue),
      C.Asset.METADATA -> metadata
    ) ++ coreJsonAttrs
}
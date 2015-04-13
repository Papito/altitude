package altitude.models

import org.joda.time.{DateTime}
import play.api.libs.json._
import altitude.{Const => C}

import scala.language.implicitConversions

object Asset {
  implicit def fromJson(json: JsValue): Asset = {
    new Asset(
      id = Some((json \ C.Asset.ID).as[String]),
      mediaType = json \ C.Asset.MEDIA_TYPE,
      locations = (json \ C.Asset.LOCATIONS).as[List[JsValue]].map(StoreLocation.fromJson),
      metadata = json \ C.Asset.METADATA
    )
  }
}

case class Asset(override val id: Option[String] = None,
                 mediaType: MediaType,
                 locations: List[StoreLocation],
                 metadata: JsValue = JsNull) extends BaseModel(id) {

  override def toJson = {
    Json.obj(
      C.Asset.LOCATIONS -> locations.map(_.toJson),
      C.Asset.MEDIA_TYPE -> (mediaType: JsValue),
      C.Asset.METADATA -> metadata
    ) ++ coreAttrs
  }
}
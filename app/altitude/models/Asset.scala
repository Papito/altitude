package altitude.models

import org.joda.time.{DateTime}
import play.api.libs.json._
import altitude.{Const => C}

import scala.language.implicitConversions

object Asset {
  implicit def fromJson(json: JsValue): Asset = {
    new Asset(
      id = (json \ C.Asset.ID).as[String],
      mediaType = json \ C.Asset.MEDIA_TYPE,
      locations = (json \ C.Asset.LOCATIONS).as[List[JsValue]].map(StoreLocation.fromJson),
      metadata = json \ C.Asset.METADATA
    )
  }
}

case class Asset(id: String = BaseModel.genId,
                 mediaType: MediaType,
                 locations: List[StoreLocation],
                 metadata: JsValue = JsNull) extends BaseModel {

  override def toJson = {
    Json.obj(
      C.Asset.ID -> id,
      C.Asset.LOCATIONS -> locations.map(_.toJson),
      C.Asset.MEDIA_TYPE -> (mediaType: JsValue),
      C.Asset.METADATA -> metadata,
      C.Asset.CREATED_AT -> isoCreatedAt,
      C.Asset.UPDATED_AT -> isoUpdatedAt
    )
  }
}
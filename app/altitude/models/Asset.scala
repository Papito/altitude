package altitude.models

import org.joda.time.{DateTime}
import play.api.libs.json
import play.api.libs.json._
import altitude.{Const => C}

import scala.language.implicitConversions
import play.api.libs.functional.syntax._

object Asset {
  implicit val writes = new Writes[Asset] {
    def writes(o: Asset) = Json.obj(
      C.Asset.LOCATIONS -> o.locations.map(_.toJson),
      C.Asset.MEDIA_TYPE -> o.mediaType.toJson,
      C.Asset.METADATA -> o.metadata // already a JsValue
    )
  }

  implicit val reads = new Reads[Asset] {
    def reads(json: JsValue): JsResult[Asset] = JsSuccess {
      Asset(
        id = (json \ C.Asset.ID).asOpt[String],
        mediaType = json \ C.Asset.MEDIA_TYPE,
        locations = (json \ C.Asset.LOCATIONS).as[List[JsObject]].map(StoreLocation.fromJson),
        metadata = json \ C.Asset.METADATA
    )}}
}

case class Asset(id: Option[String] = None,
                 mediaType: MediaType,
                 locations: List[StoreLocation],
                 metadata: JsValue = JsNull) extends BaseModel {

  def toJson = Json.toJson(this).as[JsObject]
}
package altitude.models

import play.api.libs.json.{JsValue, Json}
import scala.language.implicitConversions

object Asset {
  implicit def toJson(obj: Asset): JsValue = Json.obj(
    "id" -> obj.id,
    "mediaType" -> obj.mediaType.toJson
  )

  implicit def fromJson(json: JsValue): Asset = new Asset(
    mediaType = json \ "mediaType",
    metadata = json \ "mediaType"
  )
}

case class Asset(mediaType: MediaType, metadata: Metadata) extends BaseModel {
}
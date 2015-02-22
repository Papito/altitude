package altitude.models

import play.api.libs.json.{JsValue, JsObject, Json}

import scala.language.implicitConversions

object Asset {
  implicit def toJson(obj: Asset): JsValue = Json.obj(
    "id" -> obj.id,
    "mediaType" -> (obj.mediaType: JsValue)
  )
}

case class Asset(mediaType: MediaType, metadata: Metadata) extends BaseModel {
}
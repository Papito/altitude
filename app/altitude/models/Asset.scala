package altitude.models

import altitude.exceptions.FormatException
import play.api.libs.json.{JsResultException, JsValue, Json}

import scala.language.implicitConversions

object Asset {
  implicit def toJson(obj: Asset): JsValue = Json.obj(
    "id" -> obj.id,
    "mediaType" -> obj.mediaType.toJson
  )

  implicit def fromJson(json: JsValue): Asset = try {
    new Asset(
      mediaType = json \ "mediaType",
      metadata = json \ "mediaType"
    )
  } catch {
    case e: JsResultException => throw new FormatException(s"Cannot convert to Asset from $json")
  }
}

case class Asset(mediaType: MediaType, metadata: Metadata) extends BaseModel {
}
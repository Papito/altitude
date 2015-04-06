package altitude.models

import altitude.exceptions.FormatException
import play.api.libs.json.{JsResultException, JsValue, Json}

import scala.language.implicitConversions

object Asset {
  implicit def fromJson(json: JsValue): Asset = try {
    new Asset(
      mediaType = json \ "mediaType",
      metadata = json \ "metadata"
    )
  } catch {
    case e: JsResultException => throw new FormatException(s"Cannot convert to Asset from $json: ${e.getMessage}")
  }
}

case class Asset(mediaType: MediaType, metadata: Metadata) extends BaseModel {
  override def toJson = Json.obj(
    "id" -> id,
    "mediaType" -> mediaType.toJson,
    "metadata" -> metadata.toJson
  )
}
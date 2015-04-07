package altitude.models

import altitude.exceptions.FormatException
import play.api.libs.json.{JsResultException, JsValue, Json}

import scala.language.implicitConversions

object Asset {
  implicit def fromJson(json: JsValue): Asset = try {
    new Asset(
      objId = (json \ "id").asOpt[String],
      mediaType = json \ "mediaType",
      metadata = json \ "metadata"
    )
  } catch {
    case e: JsResultException => throw new FormatException(s"Cannot convert to from $json: ${e.getMessage}")
  }
}

case class Asset(objId: Option[String] = None, mediaType: MediaType, metadata: Metadata) extends BaseModel(objId) {

  override def toJson = Json.obj(
    "id" -> id,
    "mediaType" -> mediaType.toJson,
    "metadata" -> metadata.toJson
  )
}
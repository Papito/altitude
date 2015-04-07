package altitude.models

import altitude.exceptions.FormatException
import play.api.libs.json.{JsResultException, JsValue, Json}
import altitude.{Const => C}

import scala.language.implicitConversions

object Asset {
  implicit def fromJson(json: JsValue): Asset = try {
    new Asset(
      id = (json \ C.Asset.ID).as[String],
      mediaType = json \ C.Asset.MEDIA_TYPE,
      metadata = Some(json \ C.Asset.MEDIA_SUBTYPE)
    )
  } catch {
    case e: JsResultException => throw new FormatException(s"Cannot convert to from $json: ${e.getMessage}")
  }
}

case class Asset(override final val id: String = BaseModel.genId,
  mediaType: MediaType, metadata: Option[Metadata] = Some(new Metadata())) extends BaseModel(id) {

  override def toJson = Json.obj(
    C.Asset.ID -> id,
    C.Asset.MEDIA_TYPE -> mediaType.toJson,
    C.Asset.MEDIA_SUBTYPE -> metadata.get.toJson
  )
}
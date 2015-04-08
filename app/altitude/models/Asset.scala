package altitude.models

import altitude.exceptions.FormatException
import play.api.libs.json.{JsNull, JsResultException, JsValue, Json}
import altitude.{Const => C}

import scala.language.implicitConversions

object Asset {
  implicit def fromJson(json: JsValue): Asset = try {
    new Asset(
      id = (json \ C.Asset.ID).as[String],
      path = (json \ C.Asset.PATH).as[String],
      mediaType = json \ C.Asset.MEDIA_TYPE
      //metadata = Some(json \ C.Asset.METADATA)
    )
  } catch {
    case e: JsResultException => throw new FormatException(s"Cannot convert from $json: ${e.getMessage}")
  }
}

case class Asset(override final val id: String = BaseModel.genId,
                 mediaType: MediaType,
                 path: String,
                 metadata: Option[Metadata] = Some(new Metadata())) extends BaseModel(id) {

  override def toJson = Json.obj(
    C.Asset.ID -> id,
    C.Asset.PATH -> path,
    C.Asset.MEDIA_TYPE -> mediaType.toJson,
    C.Asset.MEDIA_SUBTYPE -> mediaType.mediaSubtype,
    C.Asset.MIME_TYPE -> mediaType.mime,
    C.Asset.METADATA -> { if (metadata.isDefined) metadata.get.toJson else JsNull }
  )
}
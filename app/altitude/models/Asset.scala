package altitude.models

import play.api.libs.json.{JsNull, JsValue, Json}
import altitude.{Const => C}

import scala.language.implicitConversions

object Asset {
  implicit def fromJson(json: JsValue): Asset = new Asset(
      id = (json \ C.Asset.ID).as[String],
      path = (json \ C.Asset.PATH).as[String],
      mediaType = json \ C.Asset.MEDIA_TYPE
      //metadata = Some(json \ C.Asset.METADATA)
    )
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
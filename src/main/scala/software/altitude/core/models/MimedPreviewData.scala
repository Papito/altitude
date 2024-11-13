package software.altitude.core.models

import org.apache.commons.codec.binary.Base64
import play.api.libs.json.JsObject
import play.api.libs.json.JsSuccess
import play.api.libs.json.JsValue
import play.api.libs.json.Json
import play.api.libs.json.OWrites
import play.api.libs.json.Reads

import scala.language.implicitConversions

object MimedPreviewData {
  final val MIME_TYPE = "image/png"
  final val FILE_EXTENSION = "png"


  implicit val reads: Reads[MimedPreviewData] = (json: JsValue) => {
    val data: String = (json \ Field.MimedData.DATA).as[String]
    JsSuccess(MimedPreviewData(
      assetId = (json \ Field.MimedData.ASSET_ID).as[String],
      data = Base64.decodeBase64(data)
    ))
  }

  implicit val writes: OWrites[MimedPreviewData] = (mimedPreviewData: MimedPreviewData) => {
    Json.obj(
      Field.MimedData.ASSET_ID -> mimedPreviewData.assetId,
      Field.MimedData.MIME_TYPE -> mimedPreviewData.mimeType,
      Field.MimedData.DATA -> Base64.encodeBase64String(mimedPreviewData.data)
    )
  }

  implicit def fromJson(json: JsValue): MimedPreviewData = Json.fromJson[MimedPreviewData](json).get
}

case class MimedPreviewData(assetId: String,
                            data: Array[Byte]) extends BaseModel with NoId with NoDates {

  val mimeType: String = MimedPreviewData.MIME_TYPE

  val toJson: JsObject = Json.toJson(this).as[JsObject]
}

package software.altitude.core.models

import software.altitude.core.{Const => C}
import org.apache.commons.codec.binary.Base64
import play.api.libs.json.{JsValue, Json}

import scala.language.implicitConversions

object Data {
  implicit def fromJson(json: JsValue): Preview = {
    val data: String = (json \ C.Data.DATA).as[String]

    Preview(
      assetId = (json \ C.Data.ASSET_ID).as[String],
      mimeType = (json \ C.Data.MIME_TYPE).as[String],
      data =  Base64.decodeBase64(data)
    )
  }
}

case class Data(assetId: String,
                mimeType: String,
                data: Array[Byte]) extends BaseModel with NoId {

  override def toJson = {
    Json.obj(
      C.Data.ASSET_ID -> assetId,
      C.Data.MIME_TYPE -> mimeType,
      C.Data.DATA -> Base64.encodeBase64String(data)
    )
  }
}

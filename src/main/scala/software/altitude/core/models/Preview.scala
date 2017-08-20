package software.altitude.core.models

import org.apache.commons.codec.binary.Base64
import play.api.libs.json.{JsValue, Json}
import software.altitude.core.{Const => C}

import scala.language.implicitConversions

object Preview {
  implicit def fromJson(json: JsValue): Preview = {
    val data: String = (json \ C.Preview.DATA).as[String]

    Preview(
      assetId = (json \ C.Preview.ASSET_ID).as[String],
      mimeType = (json \ C.Preview.MIME_TYPE).as[String],
      data =  Base64.decodeBase64(data)
    )
  }
}

case class Preview(assetId: String,
                   mimeType: String,
                   data: Array[Byte]) extends BaseModel with NoId {

  override def toJson = {
    Json.obj(
      C.Preview.ASSET_ID -> assetId,
      C.Preview.MIME_TYPE -> mimeType,
      C.Preview.DATA -> Base64.encodeBase64String(data)
    )
  }
}

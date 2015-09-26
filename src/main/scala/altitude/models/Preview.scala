package altitude.models

import altitude.{Const => C}
import org.apache.commons.codec.binary.Base64
import play.api.libs.json.{JsValue, Json}

import scala.language.implicitConversions

object Preview {
  implicit def fromJson(json: JsValue): Preview = {
    val data: String = (json \ C.Preview.DATA).as[String]

    Preview(
      id = (json \ C.Preview.ID).asOpt[String],
      assetId = (json \ C.Preview.ASSET_ID).as[String],
      mimeType = (json \ C.Preview.MIME_TYPE).as[String],
      data =  Base64.decodeBase64(data)
    ).withCoreAttr(json)
  }
}

case class Preview(id: Option[String]=None,
                   assetId: String,
                   mimeType: String,
                   data: Array[Byte]) extends BaseModel {

  override def toJson = {
    Json.obj(
      C.Preview.ASSET_ID -> assetId,
      C.Preview.MIME_TYPE -> mimeType,
      C.Preview.DATA -> Base64.encodeBase64String(data)
    ) ++ coreJsonAttrs
  }
}

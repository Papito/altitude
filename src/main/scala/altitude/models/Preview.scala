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
      asset_id = (json \ C.Preview.ASSET_ID).as[String],
      mime_type = (json \ C.Preview.MIME_TYPE).as[String],
      data =  Base64.decodeBase64(data)
    ).withCoreAttr(json)
  }
}

case class Preview(id: Option[String]=None, asset_id: String, mime_type: String, data: Array[Byte]) extends BaseModel {
  override def toJson = {
    Json.obj(
      C.Preview.ASSET_ID -> asset_id,
      C.Preview.MIME_TYPE -> mime_type,
      C.Preview.DATA -> Base64.encodeBase64String(data)
    ) ++ coreJsonAttrs
  }
}

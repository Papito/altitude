package altitude.models

import org.apache.commons.codec.binary.Base64
import play.api.libs.json.{Json, JsValue}
import altitude.{Const => C}
import scala.language.implicitConversions

object Preview {
  implicit def fromJson(json: JsValue): Preview = {
    val data: String = (json \ C.Preview.DATA).as[String]
    Preview(
      id = (json \ C.Preview.ID).asOpt[String],
      mime = (json \ C.Preview.MIME).as[String],
      data =  Base64.decodeBase64(data)
    ).withCoreAttr(json)
  }
}

case class Preview(id: Option[String], mime: String, data: Array[Byte]) extends BaseModel {
  override def toJson = {
    Json.obj(
      C.Preview.MIME -> mime,
      C.Preview.DATA -> Base64.encodeBase64String(data)
    ) ++ coreJsonAttrs
  }
}

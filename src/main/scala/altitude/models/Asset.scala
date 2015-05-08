package altitude.models

import altitude.{Const => C}
import org.apache.commons.io.FilenameUtils
import play.api.libs.json._
import org.apache.commons.codec.binary.Base64

import scala.language.implicitConversions

object Asset {
  implicit def fromJson(json: JsValue): Asset = Asset(
      id = (json \ C.Asset.ID).asOpt[String],
      mediaType = json \ C.Asset.MEDIA_TYPE,
      path = (json \ C.Asset.PATH).as[String],
      md5 = (json \ C.Asset.MD5).as[String],
      sizeBytes = (json \ C.Asset.SIZE_BYTES).as[Long],
      imageData = Base64.decodeBase64((json \ C.Asset.IMAGE_DATA).as[String]),
      metadata = json \ C.Asset.METADATA
    ).withCoreAttr(json)
}

case class Asset(id: Option[String] = None,
                 mediaType: MediaType,
                 path: String,
                 md5: String,
                 sizeBytes: Long,
                 imageData: Array[Byte] = new Array[Byte](0),
                 metadata: JsValue = JsNull) extends BaseModel {

  val fileName: String = FilenameUtils.getName(path)

  override def toJson = Json.obj(
    C.Asset.PATH -> path,
    C.Asset.MD5 -> md5,
    C.Asset.FILENAME -> fileName,
    C.Asset.SIZE_BYTES -> sizeBytes,
    C.Asset.IMAGE_DATA -> Base64.encodeBase64String(imageData),
    C.Asset.MEDIA_TYPE -> (mediaType: JsValue),
    C.Asset.METADATA -> metadata) ++ coreJsonAttrs
}
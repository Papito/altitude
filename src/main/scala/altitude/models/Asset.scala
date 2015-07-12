package altitude.models

import altitude.{Const => C}
import org.apache.commons.codec.binary.Base64
import org.apache.commons.io.FilenameUtils
import play.api.libs.json._

import scala.language.implicitConversions

object Asset {
  implicit def fromJson(json: JsValue): Asset = {
    // extract image preview, if any
    val imagePreview: Option[Array[Byte]] = (json \ C.Asset.IMAGE_PREVIEW).asOpt[String] match {
      case None | Some("") => None
      case data => Some(Base64.decodeBase64(data.get))
    }

    Asset(
      id = (json \ C.Asset.ID).asOpt[String],
      mediaType = json \ C.Asset.MEDIA_TYPE,
      path = (json \ C.Asset.PATH).as[String],
      md5 = (json \ C.Asset.MD5).as[String],
      sizeBytes = (json \ C.Asset.SIZE_BYTES).as[Long],
      imagePreview = imagePreview,
      metadata = json \ C.Asset.METADATA
    ).withCoreAttr(json)
  }
}

case class Asset(id: Option[String] = None,
                 mediaType: MediaType,
                 path: String,
                 md5: String,
                 sizeBytes: Long,
                 imagePreview: Option[Array[Byte]] = None,
                 metadata: JsValue = JsNull) extends BaseModel {

  val fileName: String = FilenameUtils.getName(path)

  override def toJson = {
    val coreJsonData: JsObject = Json.obj(
      C.Asset.PATH -> path,
      C.Asset.MD5 -> md5,
      C.Asset.FILENAME -> fileName,
      C.Asset.SIZE_BYTES -> sizeBytes,
      C.Asset.MEDIA_TYPE -> (mediaType: JsValue),
      C.Asset.METADATA -> metadata) ++ coreJsonAttrs

    imagePreview.isDefined match {
      // return with image preview if there is one
      case true => coreJsonData ++ Json.obj(
        C.Asset.IMAGE_PREVIEW -> Base64.encodeBase64String(imagePreview.get)
      )
      case false => coreJsonData
    }
  }
}
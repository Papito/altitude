package altitude.models

import altitude.{Const => C}
import org.apache.commons.io.FilenameUtils
import play.api.libs.json._

import scala.language.implicitConversions

object Asset {
  implicit def fromJson(json: JsValue): Asset = Asset(
    id = (json \ C.Base.ID).asOpt[String],
    userId = (json \ C.Base.USER_ID).as[String],
    assetType = json \ C.Asset.ASSET_TYPE,
    path = (json \ C.Asset.PATH).as[String],
    folderId = (json \ C.Asset.FOLDER_ID).as[String],
    md5 = (json \ C.Asset.MD5).as[String],
    sizeBytes = (json \ C.Asset.SIZE_BYTES).as[Long],
    metadata = Metadata.fromJson((json \ C.Asset.METADATA).as[JsObject]),
    extractedMetadata = Metadata.fromJson((json \ C.Asset.EXTRACTED_METADATA).as[JsObject])
  ).withCoreAttr(json)
}

case class Asset(id: Option[String] = None,
                 userId: String,
                 assetType: AssetType,
                 path: String,
                 md5: String,
                 sizeBytes: Long,
                 folderId: String,
                 metadata: Metadata = new Metadata(),
                 extractedMetadata: Metadata = new Metadata(),
                 previewData: Array[Byte] = new Array[Byte](0)) extends BaseModel {

  val fileName: String = FilenameUtils.getName(path)

  override def toJson = Json.obj(
    C.Base.USER_ID -> userId,
    C.Asset.PATH -> path,
    C.Asset.FOLDER_ID -> folderId,
    C.Asset.MD5 -> md5,
    C.Asset.FILENAME -> fileName,
    C.Asset.SIZE_BYTES -> sizeBytes,
    C.Asset.ASSET_TYPE -> (assetType: JsValue),
    C.Asset.METADATA -> metadata.toJson,
    C.Asset.EXTRACTED_METADATA -> extractedMetadata.toJson
  ) ++ coreJsonAttrs

  override def toString = s"path: [${this.path}] class: [${this.assetType.mediaType}:${this.assetType.mediaSubtype}]"
}
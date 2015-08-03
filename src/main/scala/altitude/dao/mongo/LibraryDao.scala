package altitude.dao.mongo

import java.io.{ByteArrayInputStream, InputStream}

import altitude.Altitude
import altitude.models.{Asset, Preview}
import altitude.transactions.TransactionId
import com.mongodb.casbah.gridfs.Imports._
import org.apache.commons.codec.binary.Base64
import org.apache.commons.io.IOUtils
import play.api.libs.json.JsObject


class LibraryDao(val app: Altitude) extends BaseMongoDao("assets") with altitude.dao.LibraryDao {
  protected def GRID_FS = GridFS(DB, "preview")

  override def addPreview(asset: Asset, bytes: Array[Byte])(implicit txId: TransactionId = new TransactionId): Option[String] = {
    if (bytes.length < 0) return None
    log.info(s"Saving preview for ${asset.path}")

    var is: Option[InputStream] = None

    try {
      is = Some(new ByteArrayInputStream(bytes))
      GRID_FS(is.get) { fh =>
        fh.filename = asset.path
        fh.contentType = asset.mediaType.mime
      }
    }
    finally {
      if (is.isDefined) is.get.close()
    }

    Some(Base64.encodeBase64String(bytes))
  }

  override def getPreview(asset_id: String)(implicit txId: TransactionId = new TransactionId): Option[Preview] = {
    log.debug(s"Getting preview for '$asset_id'")
    // get the asset
    val assetJson: Option[JsObject] = this.getById(asset_id)
    val asset: Asset = Asset.fromJson(assetJson.get)
    val gridFsFile: Option[GridFSDBFile] = GRID_FS.findOne(asset.path)

    if (gridFsFile.isEmpty)
      return None

    var is: Option[InputStream] = None

    try {
      is = Some(gridFsFile.get.inputStream)
      val bytes: Array[Byte] = IOUtils.toByteArray(is.get)
      val preview: Preview = Preview(
        id = Some(gridFsFile.get.id.toString),
        asset_id = asset_id,
        data = bytes,
        mime_type = asset.mediaType.mime)
      Some(preview)
    }
    finally {
      if (is.isDefined) is.get.close()
    }
  }
}

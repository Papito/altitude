package altitude.dao.mongo

import java.io.{ByteArrayInputStream, InputStream}

import altitude.Altitude
import altitude.models.{Asset, Preview}
import altitude.transactions.TransactionId
import com.mongodb.casbah.gridfs.Imports._
import org.apache.commons.io.IOUtils
import play.api.libs.json.JsObject

object PreviewDao {
  private var GRID_FS: JodaGridFS = null
  def initGridFs() = GRID_FS = JodaGridFS(BaseMongoDao.DB.get, "preview")
}

class PreviewDao(val app: Altitude) extends BaseMongoDao("preview") with altitude.dao.PreviewDao {
  PreviewDao.initGridFs()

  override def add(jsonIn: JsObject)(implicit txId: TransactionId): JsObject = {
    val preview: Preview = jsonIn

    val asset: Asset = app.service.asset.getById(preview.asset_id)

    if (preview.data.length == 0) throw new Exception("Preview data is empty")
    log.info(s"Saving preview for ${asset.path}")

    var is: Option[InputStream] = None

    try {
      is = Some(new ByteArrayInputStream(preview.data))
      PreviewDao.GRID_FS(is.get) { fh =>
        fh.filename = asset.path
        fh.contentType = asset.mediaType.mime
      }

      jsonIn
    }
    finally {
      if (is.isDefined) is.get.close()
    }
  }

  override def getById(asset_id: String)(implicit txId: TransactionId): Option[JsObject] = {
    val asset: Asset = app.service.asset.getById(asset_id)

    val gridFsFile: Option[JodaGridFSDBFile] = PreviewDao.GRID_FS.findOne(asset.path)

    if (gridFsFile.isEmpty)
      return None

    var is: Option[InputStream] = None

    try {
      is = Some(gridFsFile.get.inputStream)
      val bytes: Array[Byte] = IOUtils.toByteArray(is.get)
      val preview: Preview = Preview(
        id = Some(gridFsFile.get.id.toString),
        asset_id = asset.id.get,
        data = bytes,
        mime_type = asset.mediaType.mime)

      Some(preview)
    }
    finally {
      if (is.isDefined) is.get.close()
    }

  }
}
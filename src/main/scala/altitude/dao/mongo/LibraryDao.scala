package altitude.dao.mongo

import java.io.{InputStream, ByteArrayInputStream, FileInputStream}

import altitude.Altitude
import altitude.models.Asset
import com.mongodb.casbah.gridfs.Imports._


class LibraryDao(val app: Altitude) extends BaseMongoDao("assets") with altitude.dao.LibraryDao {
  protected def GRID_FS = GridFS(DB, "preview")

  def addImagePreview(asset: Asset, bytes: Array[Byte]): Asset = {
    bytes.length > 0 match {
      case false => asset
      case true =>
        log.info(s"Saving image preview for ${asset.path}")
        val is: InputStream = new ByteArrayInputStream(bytes)
        GRID_FS(is) { fh =>
          fh.filename = asset.path
          fh.contentType = asset.mediaType.mime
        }

        //FIXME: Option in a try/catch
        is.close()

        Asset(id=asset.id, mediaType=asset.mediaType, path=asset.path, md5=asset.md5,
          imagePreview=Some(bytes), sizeBytes=asset.sizeBytes, metadata=asset.metadata)
    }
  }
}

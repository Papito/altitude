package altitude.dao.mongo

import java.io.{InputStream, ByteArrayInputStream}

import altitude.Altitude
import altitude.models.Asset
import com.mongodb.casbah.gridfs.Imports._
import org.apache.commons.codec.binary.Base64


class LibraryDao(val app: Altitude) extends BaseMongoDao("assets") with altitude.dao.LibraryDao {
  protected def GRID_FS = GridFS(DB, "preview")

  override def addImagePreview(asset: Asset, bytes: Array[Byte]): Option[String] = {
    bytes.length > 0 match {
      case false => None
      case true =>
        log.info(s"Saving image preview for ${asset.path}")
        val is: InputStream = new ByteArrayInputStream(bytes)
        GRID_FS(is) { fh =>
          fh.filename = asset.path
          fh.contentType = asset.mediaType.mime
        }

        //FIXME: Option in a try/catch

        Some(Base64.encodeBase64String(bytes))
    }
  }
}

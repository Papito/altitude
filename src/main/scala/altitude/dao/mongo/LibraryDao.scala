package altitude.dao.mongo

import java.io.{InputStream, ByteArrayInputStream, FileInputStream}

import altitude.{Const => C, Altitude}
import altitude.models.Asset
import altitude.transactions.TransactionId
import org.apache.commons.codec.binary.Base64
import play.api.libs.json.{JsString, Json, JsObject}
import com.mongodb.casbah.gridfs.Imports._


class LibraryDao(val app: Altitude) extends BaseMongoDao("assets") with altitude.dao.LibraryDao {
  private val GRID_FS = GridFS(db, "preview")

  override def add(jsonIn: JsObject)(implicit txId: TransactionId): JsObject = {
    val asset: Asset = Asset.fromJson(jsonIn)

    log.debug(s"Starting LIBRARY database INSERT for $jsonIn")
    val is: InputStream = new ByteArrayInputStream(asset.imageData)

    GRID_FS(is) { fh =>
      fh.filename = asset.path
      fh.contentType = asset.mediaType.mime
    }

    //FIXME: Option in a try/catch
    is.close()

    /*
      Preview image data is not going to be saved as it's not even serialized
    */
    super.add(asset) ++ Json.obj(
      C.Asset.IMAGE_DATA -> JsString(Base64.encodeBase64String(asset.imageData))
    )
  }
}

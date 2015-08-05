package altitude.dao.mongo

import java.io.{ByteArrayInputStream, InputStream}

import altitude.Altitude
import altitude.models.{Asset, Preview}
import altitude.transactions.TransactionId
import com.mongodb.casbah.gridfs.Imports._
import org.apache.commons.io.IOUtils
import play.api.libs.json.JsObject


class AssetDao(val app: Altitude) extends BaseMongoDao("assets") with altitude.dao.AssetDao
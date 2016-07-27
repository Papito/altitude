package altitude.service

import java.io._

import altitude.Altitude
import altitude.exceptions.NotFoundException
import altitude.models.{User, Asset, Data}
import altitude.transactions.TransactionId
import org.apache.commons.io.FileUtils
import org.slf4j.LoggerFactory

class DataService(app: Altitude) {
  private final val log = LoggerFactory.getLogger(getClass)

  def getById(assetId: String)
             (implicit user: User, txId: TransactionId = new TransactionId): Data = {
    val asset: Asset = app.service.library.getById(assetId)

    val f: File = new File(asset.path)

    if (!f.isFile) {
      throw NotFoundException(s"Cannot find data for asset '$assetId'")
    }

    val byteArray = FileUtils.readFileToByteArray(f)
    val is: InputStream = new ByteArrayInputStream(byteArray)

    try {
      Data(
        assetId = assetId,
        data = byteArray,
        mimeType = "application/octet-stream")
    }
    finally {
      if (is != null) is.close()
    }
  }
}

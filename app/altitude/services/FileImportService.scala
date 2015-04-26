package altitude.services

import java.io.InputStream

import altitude.dao.{TransactionId, FileSystemImportDao}
import altitude.models.{Asset, FileImportAsset, MediaType}
import altitude.Util.log
import org.apache.commons.codec.digest.DigestUtils
import org.apache.tika.detect.{DefaultDetector, Detector}
import org.apache.tika.io.TikaInputStream
import org.apache.tika.metadata.{Metadata => TikaMetadata}
import org.apache.tika.mime.{MediaType => TikaMediaType}
import play.api.libs.json.{JsObject, JsValue}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import altitude.{Const => C}
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.io.InputStream

class FileImportService extends BaseService {
  protected val DAO = new FileSystemImportDao

  def getFilesToImport(path: String): List[FileImportAsset] = {
    log.info(s"Finding assets to import @ '$path'", C.tag.SERVICE)
    val assets = DAO.iterateAssets(path = path).toList
    log.info(s"Found ${assets.size}", C.tag.SERVICE)
    assets
  }

  def detectAssetType(importAsset: FileImportAsset): MediaType = {
    log.debug(s"Detecting media type for: '$importAsset'", C.tag.SERVICE)

    var inputStream: InputStream = null

    try {
      val url: java.net.URL = importAsset.file.toURI.toURL
      val metadata: TikaMetadata = new TikaMetadata
      inputStream = TikaInputStream.get(url, metadata)

      val detector: Detector = new DefaultDetector
      val tikaMediaType: TikaMediaType = detector.detect(inputStream, metadata)

      val assetMediaType = MediaType(
        mediaType = tikaMediaType.getType,
        mediaSubtype = tikaMediaType.getSubtype,
        mime = tikaMediaType.getBaseType.toString)

      log.debug(s"Media type for $importAsset is: $assetMediaType", C.tag.SERVICE)

      assetMediaType
    }
    finally {
      inputStream.close()
    }
  }

  def importAsset(fileAsset: FileImportAsset)(implicit txId: TransactionId = new TransactionId) : Future[Asset]  = {
    log.info(s"Importing file asset '$fileAsset'", C.tag.SERVICE)
    val mediaType = detectAssetType(fileAsset)
    val metadata: JsValue = app.service.metadata.extract(fileAsset, mediaType)

    val asset = Asset(
      path = fileAsset.absolutePath,
      md5 = getChecksum(fileAsset.absolutePath),
      mediaType = mediaType,
      metadata = metadata)

    val f = app.service.library.add(asset)
    f map { res => res }
  }

  protected def getChecksum(file: File): String = {
    var inputStream: InputStream = null

    try {
      inputStream = new FileInputStream(file)
      DigestUtils.md5Hex(inputStream)
    }
    finally  {
      inputStream.close()
    }
  }

  protected def getChecksum(path: String): String = getChecksum(new File(path))
}
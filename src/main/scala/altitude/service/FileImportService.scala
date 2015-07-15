package altitude.service

import java.io.{File, FileInputStream, InputStream}
import java.util.zip.Checksum

import altitude.dao.FileSystemImportDao
import altitude.models.{Asset, FileImportAsset, MediaType}
import altitude.transactions.TransactionId
import altitude.{Altitude, Const => C}
import org.apache.commons.codec.digest.DigestUtils
import org.apache.commons.io.FileUtils
import org.apache.tika.detect.{DefaultDetector, Detector}
import org.apache.tika.io.TikaInputStream
import org.apache.tika.metadata.{Metadata => TikaMetadata}
import org.apache.tika.mime.{MediaType => TikaMediaType}
import org.slf4j.LoggerFactory
import play.api.libs.json.JsValue

class FileImportService(app: Altitude) extends BaseService(app) {
  val log =  LoggerFactory.getLogger(getClass)

  protected val DAO = new FileSystemImportDao(app)

  protected val SUPPORTED_MEDIA_TYPES = List("audio", "image")

  def getFilesToImport(path: String): List[FileImportAsset] = {
    log.info(s"Finding assets to import @ '$path'", C.tag.SERVICE)
    val assets = DAO.iterateAssets(path = path).toList
    log.info(s"Found ${assets.size}", C.tag.SERVICE)
    assets
  }

  def detectAssetType(importAsset: FileImportAsset): MediaType = {
    log.debug(s"Detecting media type for: '$importAsset'", C.tag.SERVICE)

    var inputStream: Option[InputStream] = None

    try {
      val url: java.net.URL = importAsset.file.toURI.toURL
      val metadata: TikaMetadata = new TikaMetadata
      inputStream = Some(TikaInputStream.get(url, metadata))

      val detector: Detector = new DefaultDetector
      val tikaMediaType: TikaMediaType = detector.detect(inputStream.get, metadata)

      val assetMediaType = MediaType(
        mediaType = tikaMediaType.getType,
        mediaSubtype = tikaMediaType.getSubtype,
        mime = tikaMediaType.getBaseType.toString)

      log.debug(s"Media type for $importAsset is: $assetMediaType", C.tag.SERVICE)

      assetMediaType
    }
    finally {
      if (inputStream.isDefined) inputStream.get.close()
    }
  }

  def importAsset(fileAsset: FileImportAsset)(implicit txId: TransactionId = new TransactionId) : Option[Asset]  = {
    log.info(s"Importing file asset '$fileAsset'", C.tag.SERVICE)
    val mediaType = detectAssetType(fileAsset)

    if (!SUPPORTED_MEDIA_TYPES.contains(mediaType.mediaType)) {
      log.warn(s"Ignoring ${fileAsset.absolutePath} of type ${mediaType.mediaType}")
      return None
    }

    val metadata: JsValue = app.service.metadata.extract(fileAsset, mediaType)
    val fileSizeInBytes: Long = new File(fileAsset.absolutePath).length()

    val asset = Asset(
      path = fileAsset.absolutePath,
      md5 = getChecksum(fileAsset.absolutePath),
      mediaType = mediaType,
      sizeBytes = fileSizeInBytes,
      metadata = metadata)

    val res = app.service.library.add(asset)
    Some(res)
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
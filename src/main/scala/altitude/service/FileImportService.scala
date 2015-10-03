package altitude.service

import java.io.{File, FileInputStream, InputStream}

import altitude.dao.FileSystemImportDao
import altitude.exceptions.MetadataExtractorException
import altitude.models.{Asset, FileImportAsset, MediaType}
import altitude.transactions.TransactionId
import altitude.{Altitude, Const => C}
import org.apache.commons.codec.digest.DigestUtils
import org.apache.tika.detect.{DefaultDetector, Detector}
import org.apache.tika.io.TikaInputStream
import org.apache.tika.metadata.{Metadata => TikaMetadata}
import org.apache.tika.mime.{MediaType => TikaMediaType}
import org.slf4j.LoggerFactory
import play.api.libs.json.{Json, JsValue}

class FileImportService(app: Altitude) extends BaseService(app) {
  private final val log = LoggerFactory.getLogger(getClass)

  protected val DAO = new FileSystemImportDao(app)
  protected val SUPPORTED_MEDIA_TYPES = List("audio", "image")

  def getFilesToImport(path: String): List[FileImportAsset] = {
    log.info(s"Finding assets to import @ '$path'", C.LogTag.SERVICE)
    val assets = DAO.iterateAssets(path = path).toList
    log.info(s"Found ${assets.size}", C.LogTag.SERVICE)
    assets
  }

  def detectAssetMediaType(importAsset: FileImportAsset): MediaType = {
    log.debug(s"Detecting media type for: '$importAsset'", C.LogTag.SERVICE)

    var inputStream: Option[InputStream] = None

    try {
      val url: java.net.URL = importAsset.file.toURI.toURL
      val metadata: TikaMetadata = new TikaMetadata
      inputStream = Some(TikaInputStream.get(url, metadata))
      app.service.metadata.detectMediaTypeFromStream(inputStream.get)
    }
    finally {
      if (inputStream.isDefined) inputStream.get.close()
    }
  }

  def importAsset(fileAsset: FileImportAsset)(implicit txId: TransactionId = new TransactionId) : Option[Asset]  = {
    log.info(s"Importing file asset '$fileAsset'", C.LogTag.SERVICE)
    val mediaType = detectAssetMediaType(fileAsset)

    if (!SUPPORTED_MEDIA_TYPES.contains(mediaType.mediaType)) {
      log.warn(s"Ignoring ${fileAsset.absolutePath} of type ${mediaType.mediaType}")
      return None
    }

    var metadataParserException: Option[Exception] = None
    val metadata: JsValue = try {
      app.service.metadata.extract(fileAsset, mediaType)
    }
    catch {
      case ex: Exception => {
        log.error(s"${ex.toString} for $fileAsset")
        ex.printStackTrace()
        metadataParserException = Some(ex)
        Json.obj()
      }
    }

    val fileSizeInBytes: Long = new File(fileAsset.absolutePath).length()

    val asset = Asset(
      path = fileAsset.absolutePath,
      md5 = getChecksum(fileAsset.absolutePath),
      mediaType = mediaType,
      sizeBytes = fileSizeInBytes,
      metadata = metadata)

    val res = app.service.library.add(asset)

    // if there was a parser error, throw exception, the caller needs to know there was an error
    if (metadataParserException.isDefined) {
      throw MetadataExtractorException(res, metadataParserException.get)
    }

    Some(res)
  }

  protected def getChecksum(file: File): String = {
    var inputStream: Option[InputStream] = None

    try {
      inputStream = Some(new FileInputStream(file))
      DigestUtils.md5Hex(inputStream.get)
    }
    finally  {
      if (inputStream.isDefined) inputStream.get.close()
    }
  }

  protected def getChecksum(path: String): String = getChecksum(new File(path))
}
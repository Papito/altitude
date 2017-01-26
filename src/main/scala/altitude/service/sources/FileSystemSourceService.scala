package altitude.service.sources

import java.io.{File, FileInputStream, InputStream}

import altitude.exceptions.{FormatException, MetadataExtractorException}
import altitude.models._
import altitude.transactions.TransactionId
import altitude.{Altitude, Const => C, Context}
import org.apache.commons.codec.digest.DigestUtils
import org.apache.commons.io.FileUtils
import org.apache.commons.io.filefilter.{TrueFileFilter, IOFileFilter}
import org.apache.tika.io.TikaInputStream
import org.apache.tika.metadata.{Metadata => TikaMetadata}
import org.slf4j.LoggerFactory
import play.api.libs.json.{JsValue, Json}

class FileSystemSourceService(app: Altitude) {
  private final val log = LoggerFactory.getLogger(getClass)

  protected val SUPPORTED_MEDIA_TYPES = Set("audio", "image")

  private val ANY_FILE_FILTER: IOFileFilter = TrueFileFilter.INSTANCE

  private def iterateAssets(path: String): Iterator[FileImportAsset] = {
    require(path != null)
    log.info(s"Importing from '$path'", C.LogTag.DB)

    val files = FileUtils.iterateFiles(new File(path), ANY_FILE_FILTER, ANY_FILE_FILTER)
    new Iterable[FileImportAsset] {
      def iterator = new Iterator[FileImportAsset] {
        def hasNext = files.hasNext

        def next() = {
          val file: File = new File(files.next().toString)
          new FileImportAsset(file)
        }
      }
    }.toIterator
  }


  def getFilesToImport(path: String)
                      (implicit ctx: Context, txId: TransactionId = new TransactionId): List[FileImportAsset] = {
    log.info(s"Finding assets to import @ '$path'", C.LogTag.SERVICE)
    val assets = iterateAssets(path = path).toList
    log.info(s"Found ${assets.size}", C.LogTag.SERVICE)
    assets
  }

  def detectAssetType(importAsset: FileImportAsset): AssetType = {
    log.debug(s"Detecting media type for: '$importAsset'", C.LogTag.SERVICE)

    var inputStream: Option[InputStream] = None

    try {
      val url: java.net.URL = importAsset.file.toURI.toURL
      val metadata: TikaMetadata = new TikaMetadata
      inputStream = Some(TikaInputStream.get(url, metadata))
      app.service.metadataExtractor.detectAssetTypeFromStream(inputStream.get)
    }
    finally {
      if (inputStream.isDefined) inputStream.get.close()
    }
  }

  def importAsset(fileAsset: FileImportAsset)
                 (implicit ctx: Context, txId: TransactionId = new TransactionId) : Option[Asset]  = {
    log.info(s"Importing file asset '$fileAsset'", C.LogTag.SERVICE)
    val assetType = detectAssetType(fileAsset)

    if (!SUPPORTED_MEDIA_TYPES.contains(assetType.mediaType)) {
      log.warn(s"Ignoring ${fileAsset.absolutePath} of type ${assetType.mediaType}")
      return None
    }

    var metadataParserException: Option[Exception] = None
    val extractedMetadata: Metadata = try {
      app.service.metadataExtractor.extract(fileAsset, assetType)
    }
    catch {
      case ex: Exception =>
        metadataParserException = Some(ex)
        Json.obj()
    }

    val fileSizeInBytes: Long = new File(fileAsset.absolutePath).length()

    val asset: Asset = Asset(
      userId = ctx.user.id.get,
      path = fileAsset.absolutePath,
      md5 = getChecksum(fileAsset.absolutePath),
      assetType = assetType,
      sizeBytes = fileSizeInBytes,
      folderId = ctx.repo.uncatFolderId,
      extractedMetadata = extractedMetadata)

    var res: Option[JsValue] = None

    try {
      res = Some(app.service.library.add(asset))
    }
    catch {
      case ex: FormatException =>
        return None
    }

    // if there was a parser error, throw exception, the caller needs to know there was an error
    if (metadataParserException.isDefined) {
      throw MetadataExtractorException(asset, metadataParserException.get)
    }

    Some(Asset.fromJson(res.get))
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
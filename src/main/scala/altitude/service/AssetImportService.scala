package altitude.service

import java.io.InputStream

import altitude.models._
import altitude.transactions.TransactionId
import altitude.{Const => C, MetadataExtractorException, FormatException, Altitude, Context}
import org.apache.commons.codec.digest.DigestUtils
import org.apache.tika.io.TikaInputStream
import org.apache.tika.metadata.{Metadata => TikaMetadata}
import org.slf4j.LoggerFactory
import play.api.libs.json.{JsValue, Json}

class AssetImportService(app: Altitude) {
  private final val log = LoggerFactory.getLogger(getClass)

  protected val SUPPORTED_MEDIA_TYPES = Set("audio", "image")

  def detectAssetType(importAsset: ImportAsset): AssetType = {
    log.debug(s"Detecting media type for: '$importAsset'")

    var inputStream: Option[InputStream] = None

    try {
      val metadata: TikaMetadata = new TikaMetadata
      inputStream = Some(TikaInputStream.get(importAsset.data, metadata))
      app.service.metadataExtractor.detectAssetTypeFromStream(inputStream.get)
    }
    finally {
      if (inputStream.isDefined) inputStream.get.close()
    }
  }

  def importAsset(importAsset: ImportAsset)
                 (implicit ctx: Context, txId: TransactionId = new TransactionId) : Option[Asset]  = {
    log.info(s"Importing file asset '$importAsset'")
    val assetType = detectAssetType(importAsset)

    if (!SUPPORTED_MEDIA_TYPES.contains(assetType.mediaType)) {
      log.warn(s"Ignoring ${importAsset.path} of type ${assetType.mediaType}")
      return None
    }

    var metadataParserException: Option[Exception] = None
    val extractedMetadata: Metadata = try {
      app.service.metadataExtractor.extract(importAsset, assetType)
    }
    catch {
      case ex: Exception =>
        metadataParserException = Some(ex)
        Json.obj()
    }

    val asset: Asset = Asset(
      userId = ctx.user.id.get,
      data = importAsset.data,
      fileName = importAsset.fileName,
      md5 = getChecksum(importAsset),
      assetType = assetType,
      sizeBytes = importAsset.data.length,
      folderId = ctx.repo.triageFolderId,
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

  protected def getChecksum(importAsset: ImportAsset): String =
    DigestUtils.md5Hex(importAsset.data)
}
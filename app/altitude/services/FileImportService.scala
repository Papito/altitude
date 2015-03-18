package altitude.services

import java.io.InputStream

import altitude.dao.FileSystemImportDao
import altitude.models.{Asset, FileImportAsset, MediaType}
import altitude.util.log
import global.Altitude
import org.apache.tika.detect.{DefaultDetector, Detector}
import org.apache.tika.io.TikaInputStream
import org.apache.tika.metadata.{Metadata => TikaMetadata}
import org.apache.tika.mime.{MediaType => TikaMediaType}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future


class FileImportService {
  protected val DAO = new FileSystemImportDao
  protected val app = Altitude.getInstance()

  def getFilesToImport(path: String): List[FileImportAsset] = {
    log.info("Finding assets to import @ '$path'", Map("path" -> path))
    val assets = DAO.iterateAssets(path = path).toList
    log.info("Found $num", Map("num" -> assets.size))
    assets
  }

  def detectAssetType(importAsset: FileImportAsset): MediaType = {
    log.debug("Detecting media type for: '$asset'", Map("asset" -> importAsset))

    var inputStream: InputStream = null

    try {
      val url: java.net.URL = importAsset.file.toURI.toURL
      val metadata: TikaMetadata = new TikaMetadata
      //log.debug("Opening stream for '$asset'", Map("asset" -> importAsset))
      inputStream = TikaInputStream.get(url, metadata)

      val detector: Detector = new DefaultDetector
      val tikaMediaType: TikaMediaType = detector.detect(inputStream, metadata)

      val assetMediaType = new MediaType(
        mediaType = tikaMediaType.getType,
        mediaSubtype = tikaMediaType.getSubtype,
        mime = tikaMediaType.getBaseType.toString)

      log.info("Media type for $asset is: $mediaType",
        Map("asset" -> importAsset, "mediaType" -> assetMediaType))

      assetMediaType
    }
    finally {
      //log.debug("Closing stream for '$asset'", Map("asset" -> importAsset))
      inputStream.close()
    }
  }

  def importAsset(fileAsset: FileImportAsset): Future[Asset]  = {
    log.info("Importing file asset '$asset'", Map("asset" -> fileAsset))
    val mediaType = detectAssetType(fileAsset)
    val metadata = app.service.metadata.extract(fileAsset, mediaType)
    val asset = new Asset(mediaType = mediaType, metadata = metadata)
    val f = app.service.library.add(asset)
    f map{res => res}
  }
}
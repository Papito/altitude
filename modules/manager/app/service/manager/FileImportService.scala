package service.manager

import java.io.InputStream
import org.apache.tika.detect.{DefaultDetector, Detector}
import org.apache.tika.io.TikaInputStream
import org.apache.tika.metadata.Metadata
import org.apache.tika.mime.{MediaType => TikaMediaType}

import dao.manager.ImportDao
import models.{MediaType, FileImportAsset}
import util.log

class FileImportService {
  private val DAO = new ImportDao

  def getFilesToImport(path: String): List[FileImportAsset] = {
    require(path.nonEmpty)
    log.info("Finding assets to import @ '$path'", Map("path" -> path))
    val assets = DAO.iterateAssets(path = path).toList
    log.info("Found $num", Map("num" -> assets.size))
    assets
  }

  def detectAssetType(importAsset: FileImportAsset): MediaType = {
    log.debug("Discovering media type for: '$asset'", Map("asset" -> importAsset))

    var inputStream: InputStream = null

    try {
      val url: java.net.URL = importAsset.file.toURI.toURL
      val metadata: Metadata = new Metadata
      log.info("Opening stream for '$asset'", Map("asset" -> importAsset))
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
      log.info("Closing stream for '$asset'", Map("asset" -> importAsset))
      inputStream.close()
    }
  }
}
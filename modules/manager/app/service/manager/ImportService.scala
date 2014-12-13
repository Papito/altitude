package service.manager

import java.io.InputStream

import dao.manager.ImportDao
import models.{AssetMediaType, ImportAsset}
import org.apache.tika.detect.{DefaultDetector, Detector}
import org.apache.tika.io.TikaInputStream
import org.apache.tika.metadata.Metadata
import org.apache.tika.mime.MediaType
import util.log

class ImportService {
  private val DAO = new ImportDao

  def getImportAssets(path: String): List[ImportAsset] = {
    require(path.nonEmpty)
    log.info("Getting assets to import in '$path'", Map("path" -> path))

    this.DAO.getImportAssets(path = path)
  }

  def importAssets(path: String): Unit = {
    require(path.nonEmpty)
    log.info("Importing assets in '$path'", Map("path" -> path))

    val importAssets = getImportAssets(path)

    for (assetToImport <- importAssets) {
      importAsset(assetToImport)
    }
  }

  def importAsset(importAsset: ImportAsset): Unit = {
    log.info("Importing asset: '$asset'", Map("asset" -> importAsset))
    log.debug("Discovering media type for: '$asset'", Map("asset" -> importAsset))

    var inputStream: InputStream = null

    try {
      val url: java.net.URL = importAsset.file.toURI.toURL
      val metadata: Metadata = new Metadata
      log.info("Opening stream for '$asset'", Map("asset" -> importAsset))
      inputStream = TikaInputStream.get(url, metadata)

      val detector: Detector = new DefaultDetector
      val mediaType: MediaType = detector.detect(inputStream, metadata)

      importAsset.mediaType = new AssetMediaType(
        mediaType = mediaType.getType,
        mediaSubtype = mediaType.getSubtype,
        mime = mediaType.getBaseType.toString)

      log.info("Media type for '$asset' is $mediaType",
        Map("asset" -> importAsset, "mediaType" -> importAsset.mediaType))
    }
    finally {
      log.info("Closing stream for '$asset'", Map("asset" -> importAsset))
      inputStream.close()
    }
  }
}
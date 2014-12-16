package service.manager

import java.io.{File, InputStream}

import constants.const
import dao.manager.ImportDao
import models.{AssetMediaType, ImportAsset}
import org.apache.tika.detect.{DefaultDetector, Detector}
import org.apache.tika.io.TikaInputStream
import org.apache.tika.metadata.Metadata
import org.apache.tika.mime.MediaType
import util.log

import scala.collection.mutable.ListBuffer

class ImportService {
  private val DAO = new ImportDao

  def getImportAssets(path: String): List[ImportAsset] = {
    require(path.nonEmpty)
    log.info("Getting assets to import in '$path'", Map("path" -> path))
    DAO.iterateAssets(path = path).to[List]
  }

  def getAssetsToImport(path: String): List[ImportAsset] = {
    require(path.nonEmpty)
    log.info("Finding assets to import @ '$path'", Map("path" -> path))
    val assets = DAO.iterateAssets(path = path).toList
    log.info("Found $num", Map("num" -> assets.size))
    assets
  }

  def getAssetWithType(importAsset: ImportAsset): ImportAsset = {
    log.debug("Discovering media type for: '$asset'", Map("asset" -> importAsset))

    var inputStream: InputStream = null

    try {
      val url: java.net.URL = importAsset.file.toURI.toURL
      val metadata: Metadata = new Metadata
      log.info("Opening stream for '$asset'", Map("asset" -> importAsset))
      inputStream = TikaInputStream.get(url, metadata)

      val detector: Detector = new DefaultDetector
      val mediaType: MediaType = detector.detect(inputStream, metadata)

      val assetMediaType = new AssetMediaType(
        mediaType = mediaType.getType,
        mediaSubtype = mediaType.getSubtype,
        mime = mediaType.getBaseType.toString)

      new ImportAsset(
        file=importAsset.file,
        mediaType=assetMediaType)
    }
    finally {
      log.info("Closing stream for '$asset'", Map("asset" -> importAsset))
      inputStream.close()
    }
  }
}
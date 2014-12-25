package service.manager

import java.io.InputStream

import org.apache.tika.metadata.{Metadata => TikaMetadata}
import org.apache.tika.io.TikaInputStream
import org.apache.tika.parser.audio.AudioParser
import org.apache.tika.parser.image.ImageParser
import org.xml.sax.helpers.DefaultHandler

import models.{MediaType, Metadata}
import models.manager.FileImportAsset
import constants.{const => C}
import util.log

class MetadataService extends BaseService {
  private object PARSERS {
    val IMAGE = new ImageParser
    val AUDIO = new AudioParser
  }
  private val TIKA_HANDLER = new DefaultHandler

  def extract(importAsset: FileImportAsset, mediaType: MediaType): Metadata = {
    log.info("Extracting metadata for $asset", Map("asset" -> importAsset), C.tag.SERVICE)

    mediaType match {
      case mt: MediaType if mt.mediaType == "image" => extractImageMetadata(importAsset)
      case mt: MediaType if mt.mediaType == "audio" => extractAudioMetadata(importAsset)
      case _ => {
        log.warn(
          "No metadata extractor found for $asset of type '$mediaType'",
          Map("asset" -> importAsset, "mediaType" -> mediaType.mediaType),
          C.tag.SERVICE)
        null
      }
    }
  }

  private def extractImageMetadata(importAsset: FileImportAsset): Metadata = {
    log.info("Extracting IMAGE medadata for $asset", Map("asset" -> importAsset), C.tag.SERVICE)

    var inputStream: InputStream = null

    try {
      val url: java.net.URL = importAsset.file.toURI.toURL
      val metadata: TikaMetadata = new TikaMetadata
      log.debug("Opening stream for '$asset'", Map("asset" -> importAsset))
      inputStream = TikaInputStream.get(url, metadata)

      PARSERS.IMAGE.parse(inputStream, TIKA_HANDLER, metadata, null)
      for (key <- metadata.names()) {
        println(key + ": " + metadata.get(key))
      }

      new Metadata
    }
    finally {
      log.debug("Closing stream for '$asset'", Map("asset" -> importAsset))
      if (inputStream != null)
        inputStream.close()
    }
  }

  private def extractAudioMetadata(importAsset: FileImportAsset): Metadata = {
    log.info("Extracting AUDIO medadata for $asset", Map("asset" -> importAsset), C.tag.SERVICE)
    var inputStream: InputStream = null

    try {
      val url: java.net.URL = importAsset.file.toURI.toURL
      val metadata: TikaMetadata = new TikaMetadata
      log.debug("Opening stream for '$asset'", Map("asset" -> importAsset))
      inputStream = TikaInputStream.get(url, metadata)

      PARSERS.AUDIO.parse(inputStream, TIKA_HANDLER, metadata, null)
      for (key <- metadata.names()) {
        println(key + ": " + metadata.get(key))
      }

      new Metadata
    }
    finally {
      log.debug("Closing stream for '$asset'", Map("asset" -> importAsset))
      if (inputStream != null)
        inputStream.close()
    }
  }
}

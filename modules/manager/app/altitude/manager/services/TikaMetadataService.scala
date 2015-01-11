package altitude.manager.services

import java.io.InputStream

import altitude.common.models.{MediaType, Metadata}
import altitude.manager.models.FileImportAsset
import altitude.util.log
import altitude.{Const => C}
import org.apache.tika.io.TikaInputStream
import org.apache.tika.metadata.{Metadata => TikaMetadata}
import org.apache.tika.parser.AbstractParser
import org.apache.tika.parser.audio.AudioParser
import org.apache.tika.parser.image.ImageParser
import org.apache.tika.parser.mp3.Mp3Parser
import org.xml.sax.helpers.DefaultHandler

import scala.collection.immutable.HashMap

class TikaMetadataService extends AbstractMetadataService {

  private object PARSERS {
    final val IMAGE = new ImageParser
    final val MPEG_AUDIO = new Mp3Parser
    final val SIMPLE_AUDIO = new AudioParser
  }

  final private val TIKA_HANDLER = new DefaultHandler

  override def extract(importAsset: FileImportAsset, mediaType: MediaType): Metadata = mediaType match {
    case mt: MediaType if mt.mediaType == "image" =>
      extractMetadata(importAsset, PARSERS.IMAGE)
    case mt: MediaType if mt.mediaType == "audio" && mt.mediaSubtype == "mpeg" =>
      extractMetadata(importAsset, PARSERS.MPEG_AUDIO)
    case mt: MediaType if mt.mediaType == "audio" =>
      extractMetadata(importAsset, PARSERS.SIMPLE_AUDIO)
    case _ => {
      log.warn(
        "No metadata extractor found for $asset of type '$mediaType'",
        Map("asset" -> importAsset, "mediaType" -> mediaType.mediaType),
        C.tag.SERVICE)
      null
    }
  }

  private def extractMetadata(importAsset: FileImportAsset, parser: AbstractParser): Metadata = {
    log.info(
      "Extracting metadata for '$asset' with $parserType",
      Map("asset" -> importAsset, "parserType" -> parser.getClass.getSimpleName),
      C.tag.SERVICE)

    var inputStream: InputStream = null

    try {
      val url: java.net.URL = importAsset.file.toURI.toURL
      val metadata: TikaMetadata = new TikaMetadata
      //log.debug("Opening stream for '$asset'", Map("asset" -> importAsset))
      inputStream = TikaInputStream.get(url, metadata)

      parser.parse(inputStream, TIKA_HANDLER, metadata, null)

      // accumulate metadata into a map
      val data = metadata.names().foldLeft(new HashMap[String, String])(
        (col, key) => col + (key -> metadata.get(key))
      )

      new Metadata(data)
    }
    finally {
      //log.debug("Closing stream for '$asset'", Map("asset" -> importAsset))
      if (inputStream != null)
        inputStream.close()
    }
  }
}

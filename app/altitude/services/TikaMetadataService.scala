package altitude.services

import java.io.{StringWriter, InputStream}

import altitude.models.{FileImportAsset, MediaType}
import altitude.util.log
import altitude.{Const => C}
import org.apache.tika.io.TikaInputStream
import org.apache.tika.metadata.{Metadata => TikaMetadata}
import org.apache.tika.parser.AbstractParser
import org.apache.tika.parser.audio.AudioParser
import org.apache.tika.parser.image.ImageParser
import org.apache.tika.parser.mp3.Mp3Parser
import org.apache.tika.metadata.serialization.JsonMetadata
import org.xml.sax.helpers.DefaultHandler
import play.api.libs.json.{Json, JsNull, JsValue}

class TikaMetadataService extends AbstractMetadataService {

  private object PARSERS {
    final val IMAGE = new ImageParser
    final val MPEG_AUDIO = new Mp3Parser
    final val SIMPLE_AUDIO = new AudioParser
  }

  final private val TIKA_HANDLER = new DefaultHandler

  override def extract(importAsset: FileImportAsset, mediaType: MediaType): JsValue = mediaType match {
    case mt: MediaType if mt.mediaType == "image" =>
      extractMetadata(importAsset, PARSERS.IMAGE)
    case mt: MediaType if mt.mediaType == "audio" && mt.mediaSubtype == "mpeg" =>
      extractMetadata(importAsset, PARSERS.MPEG_AUDIO)
    case mt: MediaType if mt.mediaType == "audio" =>
      extractMetadata(importAsset, PARSERS.SIMPLE_AUDIO)
    case _ => {
      log.warn(s"No metadata extractor found for $importAsset of type '$mediaType'", C.tag.SERVICE)
      JsNull
    }
  }

  private def extractMetadata(importAsset: FileImportAsset, parser: AbstractParser): JsValue = {
    log.info(
      "Extracting metadata for '$asset' with $parserType",
      Map("asset" -> importAsset, "parserType" -> parser.getClass.getSimpleName),
      C.tag.SERVICE)

    var inputStream: InputStream = null
    val writer: StringWriter = new StringWriter()

    try {
      val url: java.net.URL = importAsset.file.toURI.toURL
      val metadata: TikaMetadata = new TikaMetadata
      inputStream = TikaInputStream.get(url, metadata)

      parser.parse(inputStream, TIKA_HANDLER, metadata, null)

      JsonMetadata.toJson(metadata, writer) // raises org.apache.tika.exception.TikaException
      val jsonData = writer.toString
      val json = Json.parse(jsonData)
      println(Json.prettyPrint(json))
      json
    }
    finally {
      writer.close()
      if (inputStream != null)
        inputStream.close()
    }
  }
}

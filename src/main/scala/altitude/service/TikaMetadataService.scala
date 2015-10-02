package altitude.service

import java.io.{InputStream, StringWriter}

import altitude.exceptions.MetadataExtractorException
import altitude.models.MediaType
import altitude.models.{FileImportAsset, MediaType}
import altitude.{Const => C}
import org.apache.tika.detect.{DefaultDetector, Detector}
import org.apache.tika.exception.TikaException
import org.apache.tika.io.TikaInputStream
import org.apache.tika.metadata.serialization.JsonMetadata
import org.apache.tika.metadata.{Metadata => TikaMetadata}
import org.apache.tika.mime.{MediaType => TikaMediaType}
import org.apache.tika.parser.{AutoDetectParser, AbstractParser}
import org.apache.tika.parser.audio.AudioParser
import org.apache.tika.parser.image.ImageParser
import org.apache.tika.parser.mp3.Mp3Parser
import org.slf4j.LoggerFactory
import org.xml.sax.helpers.DefaultHandler
import play.api.libs.json.{JsNull, JsValue, Json}
import org.apache.tika.parser.jpeg.JpegParser

class TikaMetadataService extends AbstractMetadataService {
  val log =  LoggerFactory.getLogger(getClass)

  private object PARSERS {
    final val IMAGE = new ImageParser
    final val MPEG_AUDIO = new Mp3Parser
    final val ANY_AUDIO = new AudioParser
  }

  final private val TIKA_HANDLER = new DefaultHandler

  override def extract(importAsset: FileImportAsset, mediaType: MediaType): JsValue = mediaType match {
    case mt: MediaType if mt.mediaType == "image" =>
      extractMetadata(importAsset, new JpegParser)
    case mt: MediaType if mt.mediaType == "audio" && mt.mediaSubtype == "mpeg" =>
      extractMetadata(importAsset, PARSERS.MPEG_AUDIO)
    case mt: MediaType if mt.mediaType == "audio" =>
      extractMetadata(importAsset, PARSERS.ANY_AUDIO)
    case _ =>
      log.warn(s"No metadata extractor found for $importAsset of type '$mediaType'", C.LogTag.SERVICE)
      JsNull
  }

  private def extractMetadata(importAsset: FileImportAsset, parser: AbstractParser): JsValue = {
    log.info(s"Extracting metadata for '$importAsset' with ${parser.getClass.getSimpleName}")

    var inputStream: Option[InputStream] = None
    var writer: Option[StringWriter] = None

    try {
      try {
        val url: java.net.URL = importAsset.file.toURI.toURL
        val metadata: TikaMetadata = new TikaMetadata
        inputStream = Some(TikaInputStream.get(url, metadata))
        writer = Some(new StringWriter())

        parser.parse(inputStream.get, TIKA_HANDLER, metadata, null)

        JsonMetadata.toJson(metadata, writer.get)
      }
      catch {
        case ex: Throwable => throw new MetadataExtractorException(ex)
      }

      val jsonData = writer.get.toString
      Json.parse(jsonData)
    }
    finally {
      if (writer.isDefined)
        writer.get.close()
      if (inputStream.isDefined)
        inputStream.get.close()
    }
  }

  def detectMediaTypeFromStream(is: InputStream): MediaType = {
    val metadata: TikaMetadata = new TikaMetadata

    val detector: Detector = new DefaultDetector
    val tikaMediaType: TikaMediaType = detector.detect(is, metadata)

    val assetMediaType = MediaType(
      mediaType = tikaMediaType.getType,
      mediaSubtype = tikaMediaType.getSubtype,
      mime = tikaMediaType.getBaseType.toString)

    assetMediaType
  }
}

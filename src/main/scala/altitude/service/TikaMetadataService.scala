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
import org.apache.tika.parser.image.TiffParser

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
      extractMetadata(importAsset, List(new JpegParser, new TiffParser))
/*
    case mt: MediaType if mt.mediaType == "audio" && mt.mediaSubtype == "mpeg" =>
      extractMetadata(importAsset, List(PARSERS.MPEG_AUDIO))
    case mt: MediaType if mt.mediaType == "audio" =>
      extractMetadata(importAsset, List(PARSERS.ANY_AUDIO))
*/
    case _ =>
      log.warn(s"No metadata extractor found for $importAsset of type '$mediaType'", C.LogTag.SERVICE)
      JsNull
  }

  private def extractMetadata(importAsset: FileImportAsset, parsers: List[AbstractParser]): JsValue = {
    var inputStream: Option[InputStream] = None
    var writer: Option[StringWriter] = None

    class AllDone extends Exception

    var metadata: Option[TikaMetadata] = None

    try {
      for (parser <- parsers) {
        log.info(s"Extracting metadata for '$importAsset' with ${parser.getClass.getSimpleName}")
        metadata = None

        try {
          val url: java.net.URL = importAsset.file.toURI.toURL
          metadata = Some(new TikaMetadata)
          inputStream = Some(TikaInputStream.get(url, metadata.get))
          writer = Some(new StringWriter())

          parser.parse(inputStream.get, TIKA_HANDLER, metadata.get, null)
          throw new AllDone
        }
        catch {
          case ex: AllDone => throw ex
          case ex: Exception => {
            ex.printStackTrace()
            log.error(
              s"Error extracting metadata for '$importAsset' with ${parser.getClass.getSimpleName}: ${ex.toString}")
          }
        }
        finally {
          if (writer.isDefined)
            writer.get.close()
          if (inputStream.isDefined)
            inputStream.get.close()
        }
      }
    }
    catch {
      case ex: AllDone =>
    }

    metadata match {
      case None => {
        throw new Exception(s"All matching metadata parsers failed for $importAsset")
      }
      case _ => {
        JsonMetadata.toJson(metadata.get, writer.get)
        val jsonData = writer.get.toString
        Json.parse(jsonData)
      }
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

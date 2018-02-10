package software.altitude.core.service

import java.io.InputStream

import org.apache.tika.detect.{DefaultDetector, Detector}
import org.apache.tika.io.TikaInputStream
import org.apache.tika.metadata.{Metadata => TikaMetadata}
import org.apache.tika.mime.{MediaType => TikaMediaType}
import org.apache.tika.parser.AbstractParser
import org.apache.tika.parser.audio.AudioParser
import org.apache.tika.parser.image.{ImageParser, TiffParser}
import org.apache.tika.parser.jpeg.JpegParser
import org.apache.tika.parser.mp3.Mp3Parser
import org.slf4j.LoggerFactory
import org.xml.sax.helpers.DefaultHandler
import software.altitude.core.models.{MetadataValue, AssetType, ImportAsset, Metadata}
import software.altitude.core.{AllDone, Const => C}

class TikaMetadataExtractionService extends MetadataExtractionService {
  val log =  LoggerFactory.getLogger(getClass)

  private object PARSERS {
    final val IMAGE = new ImageParser
    final val MPEG_AUDIO = new Mp3Parser
    final val ANY_AUDIO = new AudioParser
  }

  final private val TIKA_HANDLER = new DefaultHandler

  override def extract(importAsset: ImportAsset, mediaType: AssetType, asRaw: Boolean = false): Metadata = {
    val raw: Option[TikaMetadata]  = mediaType match {
      case mt: AssetType if mt.mediaType == "image" =>
        extractMetadata(importAsset, List(new JpegParser, new TiffParser))
      /*
          case mt: MediaType if mt.mediaType == "audio" && mt.mediaSubtype == "mpeg" =>
            extractMetadata(importAsset, List(PARSERS.MPEG_AUDIO))
          case mt: MediaType if mt.mediaType == "audio" =>
            extractMetadata(importAsset, List(PARSERS.ANY_AUDIO))
      */
      case _ =>
        log.warn(s"No metadata extractor found for $importAsset of type '$mediaType'", C.LogTag.SERVICE)
        None
    }

    val metadata = rawToMetadata(raw)

    // make it nice and neat out of the horrible mess that this probably is
    if (asRaw) metadata else normalize(metadata)
  }

  private def rawToMetadata(raw: Option[TikaMetadata]): Metadata = {
    if (raw.isEmpty) {
      return Metadata()
    }

    val data = scala.collection.mutable.Map[String, Set[String]]()

    for (name <- raw.get.names()) {
      data(name) = Set(raw.get.get(name).trim)
    }

    Metadata(data.toMap)
  }

  private def extractMetadata(importAsset: ImportAsset, parsers: List[AbstractParser]): Option[TikaMetadata]  = {
    var inputStream: Option[InputStream] = None
    var metadata: Option[TikaMetadata] = None

    try {
      for (parser <- parsers) {
        log.info(s"Extracting metadata for '$importAsset' with ${parser.getClass.getSimpleName}")
        metadata = None

        try {
          metadata = Some(new TikaMetadata)
          inputStream = Some(TikaInputStream.get(importAsset.data, metadata.get))
          parser.parse(inputStream.get, TIKA_HANDLER, metadata.get, null)
          throw AllDone()
        }
        catch {
          case ex: AllDone => throw ex
          case ex: Exception =>
            ex.printStackTrace()
            log.error(
              s"Error extracting metadata for '$importAsset' with ${parser.getClass.getSimpleName}: ${ex.toString}")
        }
        finally {
          if (inputStream.isDefined)
            inputStream.get.close()
        }
      }
    }
    catch {
      case ex: AllDone =>
    }

    metadata
  }

  private def normalize(metadata: Metadata): Metadata = {
    val normalizedData = scala.collection.mutable.Map[String, Set[MetadataValue]]()

    FIELD_BIBLE.foreach { case (destField, srcFields) =>
      srcFields.isEmpty match {
        // if the destination field is the same as the source
        case true if metadata.contains(destField) =>
          normalizedData(destField) = metadata.data(destField)

        case _ =>
          // find all the fields that exist in metadata
          val existingSrcFields = srcFields.filter(metadata.contains)
          // the field on TOP is it
          if (existingSrcFields.nonEmpty) {
            normalizedData(destField) = metadata.data(existingSrcFields.head)
          }
      }
    }
    Metadata(normalizedData.toMap)
  }

  def detectAssetTypeFromStream(is: InputStream): AssetType = {
    val metadata: TikaMetadata = new TikaMetadata

    val detector: Detector = new DefaultDetector
    val tikaMediaType: TikaMediaType = detector.detect(is, metadata)

    val assetType = AssetType(
      mediaType = tikaMediaType.getType,
      mediaSubtype = tikaMediaType.getSubtype,
      mime = tikaMediaType.getBaseType.toString)

    assetType
  }
}

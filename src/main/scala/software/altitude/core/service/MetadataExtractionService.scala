package software.altitude.core.service

import org.apache.tika.detect.DefaultDetector
import org.apache.tika.detect.Detector
import org.apache.tika.io.TikaInputStream
import org.apache.tika.metadata.{Metadata => TikaMetadata}
import org.apache.tika.mime.{MediaType => TikaMediaType}
import org.apache.tika.parser.AutoDetectParser
import org.apache.tika.sax.BodyContentHandler
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import software.altitude.core.Altitude
import software.altitude.core.models.AssetType
import software.altitude.core.models.Metadata
import software.altitude.core.models.MetadataValue

import java.io.InputStream

class MetadataExtractionService(app: Altitude) {
  protected final val logger: Logger = LoggerFactory.getLogger(getClass)

  /**
   * This structure defines how we construct the final metadata object.
   * Metadata can have a lot of fields that are related or identical
   */
  private val FIELD_REFERENCE: Map[String, List[String]] = Map(
    /*
        FINAL_FIELD_1 -> [
          POSSIBLE_FIELD_1_PRIORITY_1,
          POSSIBLE_FIELD_1_PRIORITY_2]
        FINAL_FIELD_2 -> [
          POSSIBLE_FIELD_2_PRIORITY_1,
          POSSIBLE_FIELD_2_PRIORITY_2,
          POSSIBLE_FIELD_2_PRIORITY_3]
     */
    "Image Width" -> List(
      "tiff:ImageWidth",
      "exif:ImageWidth",
      "Image Width"
    ),

    "Image Height" -> List(
      "tiff:ImageLength",
      "exif:Image Height",
      "Image Height"
    ),

    "Make" -> List(
      "tiff:Make",
      "exif:Make",
      "Make"
    ),

    "Model" -> List(
      "tiff:Model",
      "exif:Model",
      "Model"
    ),

    "Software" -> List(),

    "Lens" -> List(
      "Lens Information",
      "Lens",
      "Lens Model"
    ),

    "Iso Speed" -> List(
      "exif:IsoSpeedRatings",
      "ISO Speed Ratings"),

    "Focal Length" -> List(
      "exif:FocalLength",
      "Focal Length",
      "Aperture Value"
    ),

    "F-Number" -> List(
      "exif:FNumber",
      "F-Number"),

    "Exposure Time" -> List(
      "exif:ExposureTime",
      "Exposure Time"
    ),

    "Flash" -> List(
      "exif:Flash",
      "Flash"
    ),

    "User Comment" -> List(
      "User Comment",
      "w:comments",
      "JPEG Comment",
      "Comments",
      "Comment")
  )

  def extract(data: Array[Byte]): Metadata = {
    val raw: Option[TikaMetadata] = extractMetadata(data)

    rawToMetadata(raw)
  }

  private def rawToMetadata(raw: Option[TikaMetadata]): Metadata = {
    if (raw.isEmpty) {
      Metadata()
    }

    val data = scala.collection.mutable.Map[String, Set[String]]()

    for (name <- raw.get.names()) {
      data(name) = Set(raw.get.get(name).trim)
    }

    Metadata(data.toMap)
  }

  private def extractMetadata(data: Array[Byte]): Option[TikaMetadata] = {
    logger.info("Extracting metadata")
    try {
      val metadata = new TikaMetadata()
      val inputStream = TikaInputStream.get(data, metadata)
      val parser = new AutoDetectParser()
      val handler = new BodyContentHandler()

      parser.parse(inputStream, handler, metadata)
      inputStream.close()
      Some(metadata)
    }
    catch {
      case ex: Exception =>
        ex.printStackTrace()
        logger.error(s"Error extracting metadata: ${ex.toString}")
        None
      }
  }

  private def normalize(metadata: Metadata): Metadata = {
    val normalizedData = scala.collection.mutable.Map[String, Set[MetadataValue]]()

    FIELD_REFERENCE.foreach { case (destField, srcFields) =>
      if (srcFields.isEmpty && metadata.contains(destField)) {
        normalizedData(destField) = metadata.data(destField)
      }
      else {
        // find all the fields that exist in metadata
        val existingSrcFields = srcFields.filter(metadata.contains)
        // the field on TOP is it
        if (existingSrcFields.nonEmpty) {
          normalizedData(destField) = metadata.data(existingSrcFields.head)
        }
      }
    }

    // copy over the raw fields in
    Metadata(normalizedData.toMap)
  }

  def detectAssetType(data: Array[Byte]): AssetType = {
    var inputStream: Option[InputStream] = None

    try {
      val metadata: TikaMetadata = new TikaMetadata
      inputStream = Some(TikaInputStream.get(data, metadata))

      val detector: Detector = new DefaultDetector
      val tikaMediaType: TikaMediaType = detector.detect(inputStream.get, metadata)

      val assetType = AssetType(
        mediaType = tikaMediaType.getType,
        mediaSubtype = tikaMediaType.getSubtype,
        mime = tikaMediaType.getBaseType.toString)

      assetType
    }
    finally {
      if (inputStream.isDefined) inputStream.get.close()
    }
  }
}

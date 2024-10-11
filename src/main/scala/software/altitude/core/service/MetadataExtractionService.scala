package software.altitude.core.service

import com.drew.imaging.ImageMetadataReader
import com.drew.metadata.Directory
import org.apache.tika.detect.DefaultDetector
import org.apache.tika.detect.Detector
import org.apache.tika.io.TikaInputStream
import org.apache.tika.metadata.{Metadata => TikaMetadata}
import org.apache.tika.mime.{MediaType => TikaMediaType}
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import software.altitude.core.Altitude
import software.altitude.core.models.AssetType
import software.altitude.core.models.Metadata
import scala.jdk.CollectionConverters._

import java.io.{ByteArrayInputStream, InputStream}

class MetadataExtractionService(app: Altitude) {
  protected final val logger: Logger = LoggerFactory.getLogger(getClass)

  def extract(data: Array[Byte]): Metadata = {
    val rawMetadata: com.drew.metadata.Metadata = ImageMetadataReader.readMetadata(new ByteArrayInputStream(data))
    println(rawMetadata)

    for (directory: Directory <- rawMetadata.getDirectories.asScala) {
      for (tag <- directory.getTags.asScala) {
        println(tag.getTagName + " = " + tag.getDescription)
      }
    }

    Metadata()
    // rawToMetadata(raw)
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

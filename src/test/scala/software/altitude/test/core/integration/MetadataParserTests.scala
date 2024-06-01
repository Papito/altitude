package software.altitude.test.core.integration

import java.io.File

import org.scalatest.DoNotDiscover
import org.scalatest.matchers.must.Matchers.{contain, be}
import org.scalatest.matchers.should.Matchers.convertToAnyShouldWrapper
import software.altitude.core.models.Metadata

@DoNotDiscover class MetadataParserTests(val config: Map[String, Any]) extends IntegrationTestCore {

  test("normalize metadata") {
    val metadata: Metadata = getMetadata("images/6.jpg")

    val verify = Map(
      "Exposure Mode" -> "Auto exposure",
      "Exposure Program" -> "Landscape mode",
      "Image Height" -> "8",
      "Image Width" -> "10",
      "exif:ExposureTime" -> "0.0025",
      "exif:FNumber" -> "8.0",
      "exif:Flash" -> "false",
      "exif:FocalLength" -> "16.3",
      "exif:IsoSpeedRatings" -> "100",
      "tiff:Make" -> "FUJIFILM",
      "tiff:Model" -> "FinePix F50fd",
      "tiff:Orientation" -> "1",
      "tiff:Software" -> "GIMP 2.8.10")

    verify.foreach { case (k, v) =>
      metadata.data.keys.toSeq should contain(k)
      metadata(k).map(_.value) should contain (v)
    }

    metadata.get("X Resolution") shouldNot be(None)
    metadata("X Resolution").map(_.value) should contain ("72 dots per inch")
    metadata.get("Y Resolution") shouldNot be(None)
    metadata("Y Resolution").map(_.value) should contain ("72 dots per inch")
  }

  private def getMetadata(p: String): Metadata = {
    val path = getClass.getResource(s"/import/$p").getPath
    val importAsset = fileToImportAsset(new File(path))
    val mediaType = altitude.service.assetImport.detectAssetType(importAsset)
    altitude.service.metadataExtractor.extract(importAsset, mediaType)
  }
}

package integration

import java.io.File

import altitude.models.FileImportAsset
import org.scalatest.DoNotDiscover
import org.scalatest.Matchers._
import play.api.libs.json.JsValue

@DoNotDiscover class MetadataParserTests(val config: Map[String, String]) extends IntegrationTestCore {

  test("normalize metadata") {
    val metadata = getMetadata("images/6.jpg")

    val verify = Map(
      "Exposure Mode" -> "Auto exposure",
      "Exposure Program" -> "Landscape mode",
      "Image Height" -> "8 pixels",
      "Image Width" -> "10 pixels",
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
      (metadata \ k).asOpt[String] should contain(v)
    }

    (metadata \ "X Resolution").asOpt[String] shouldNot be(None)
    (metadata \ "X Resolution").as[String].startsWith("72 dots") shouldBe true
    (metadata \ "Y Resolution").asOpt[String] shouldNot be(None)
    (metadata \ "Y Resolution").as[String].startsWith("72 dots") shouldBe true
  }

  private def getMetadata(p: String): JsValue = {
    val path = getClass.getResource(s"../import/$p").getPath
    val fileImportAsset = new FileImportAsset(new File(path))
    val mediaType = altitude.service.fileImport.detectAssetMediaType(fileImportAsset)
    altitude.service.metadata.extract(fileImportAsset, mediaType)
  }

}

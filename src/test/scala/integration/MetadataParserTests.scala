package integration

import java.io.File

import altitude.models.{FileImportAsset}
import org.scalatest.DoNotDiscover
import org.scalatest.Matchers._
import play.api.libs.json.{Json, JsValue}

@DoNotDiscover class MetadataParserTests(val config: Map[String, String]) extends IntegrationTestCore {

  test("EXIF 1") {
    val metadata = getMetadata("images/exif/DSC_0111.jpg")
    println(Json.prettyPrint(metadata))
  }

  private def getMetadata(p: String): JsValue = {
    val path = getClass.getResource(s"../files/incoming/$p").getPath
    val fileImportAsset = new FileImportAsset(new File(path))
    val mediaType = altitude.service.fileImport.detectAssetType(fileImportAsset)
    altitude.service.metadata.extract(fileImportAsset, mediaType)
  }

}

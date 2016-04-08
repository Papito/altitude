package integration

import java.io.File

import altitude.models.FileImportAsset
import org.scalatest.DoNotDiscover
import play.api.libs.json.{Json, JsValue}

@DoNotDiscover class MetadataParserTests(val config: Map[String, String]) extends IntegrationTestCore {

  test("get metadata") {
    // just make sure there is no exception
    getMetadata("images/6.jpg")
  }

  test("normalize metadata") {
    val metadata = getMetadata("images/6.jpg")
    println(Json.prettyPrint(metadata))
  }

  private def getMetadata(p: String): JsValue = {
    val path = getClass.getResource(s"../import/$p").getPath
    val fileImportAsset = new FileImportAsset(new File(path))
    val mediaType = altitude.service.fileImport.detectAssetMediaType(fileImportAsset)
    altitude.service.metadata.extract(fileImportAsset, mediaType)
  }

}

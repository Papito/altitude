package integration

import java.io.File

import altitude.exceptions.DuplicateException
import altitude.models.{Asset, FileImportAsset}
import org.scalatest.DoNotDiscover

import org.scalatest.Matchers._

@DoNotDiscover class ImportTests(val config: Map[String, _]) extends IntegrationTestCore {
  test("import image (JPEG)") {
    val asset = importFile("images/1.jpg")
    asset.imageData.length should not be(0)
  }

  test("import audio (MP3)") {
    val asset = importFile("audio/all.mp3")
    (asset.metadata \ "Author").as[String] should equal("Whitney Houston")
  }

  test("import file list") {
    val incomingPath = getClass.getResource("../files/incoming").getPath
    val assets = altitude.service.fileImport.getFilesToImport(path=incomingPath)
    assets should not be empty
  }


  test("import duplicate") {
    importFile("images/1.jpg")
    val path = getClass.getResource(s"../files/incoming/images/1.jpg").getPath
    val fileImportAsset = new FileImportAsset(new File(path))

    intercept[DuplicateException] {
      altitude.service.fileImport.importAsset(fileImportAsset)    }
  }


  protected def importFile(p: String): Asset = {
    val path = getClass.getResource(s"../files/incoming/$p").getPath
    val fileImportAsset = new FileImportAsset(new File(path))
    val importedAsset = altitude.service.fileImport.importAsset(fileImportAsset).get
    importedAsset.mediaType should equal(importedAsset.mediaType)
    importedAsset.path should not be empty
    importedAsset.md5 should not be empty
    importedAsset.createdAt should not be None

    val asset = altitude.service.library.getById(importedAsset.id.get).get: Asset
    asset.mediaType should equal(importedAsset.mediaType)
    asset.path should not be empty
    asset.md5 should not be empty
    asset.createdAt should not be None
    asset
  }

}

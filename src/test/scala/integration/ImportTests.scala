package integration

import java.io.File

import altitude.exceptions.DuplicateException
import altitude.models.{Asset, FileImportAsset, Preview}
import org.scalatest.DoNotDiscover
import org.scalatest.Matchers._

@DoNotDiscover class ImportTests(val config: Map[String, String]) extends IntegrationTestCore {

  test("import audio (MP3)") {
    val asset = importFile("audio/all.mp3")
    val optAuthor = (asset.metadata \ "Author").asOpt[String]
    optAuthor should not be None
    optAuthor should equal("Whitney Houston")
  }

/*
  test("import audio images (MP3)") {
    val asset = importFile("audio/images.mp3")
  }


  test("import file list") {
    val incomingPath = getClass.getResource("../files/incoming").getPath
    val assets = altitude.service.fileImport.getFilesToImport(path=incomingPath)
    assets should not be empty
  }

  test("import image (JPEG)") {
    val asset = importFile("images/1.jpg")
    val preview: Option[Preview] = altitude.service.library.getPreview(asset.id.get)
    preview should not be None
    preview.get.mime should equal("image/jpeg")
    preview.get.data.length should not be 0
  }

  test("import duplicate") {
    importFile("images/1.jpg")
    val path = getClass.getResource(s"../files/incoming/images/1.jpg").getPath
    val fileImportAsset = new FileImportAsset(new File(path))

    intercept[DuplicateException] {
      altitude.service.fileImport.importAsset(fileImportAsset)    }
  }
*/

  protected def importFile(p: String): Asset = {
    val path = getClass.getResource(s"../files/incoming/$p").getPath
    val fileImportAsset = new FileImportAsset(new File(path))
    val importedAsset = altitude.service.fileImport.importAsset(fileImportAsset).get
    importedAsset.createdAt should not be None
    importedAsset.mediaType should equal(importedAsset.mediaType)
    importedAsset.path should not be empty
    importedAsset.md5 should not be empty

    val asset = altitude.service.library.getById(importedAsset.id.get).get: Asset
    asset.mediaType should equal(importedAsset.mediaType)
    asset.path should not be empty
    asset.md5 should not be empty
    asset.createdAt should not be None
    asset
  }

}

package integration

import java.io.File

import altitude.DuplicateException
import altitude.models.{Asset, Preview}
import org.scalatest.DoNotDiscover
import org.scalatest.Matchers._

@DoNotDiscover class FileSystemImportTests(val config: Map[String, Any]) extends IntegrationTestCore {

  test("import duplicate") {
    importFile("images/1.jpg")
    val path = getClass.getResource(s"../import/images/1.jpg").getPath
    val importAsset = altitude.service.source.fileSystem.fileToImportAsset(new File(path))

    intercept[DuplicateException] {
      altitude.service.assetImport.importAsset(importAsset)
    }
  }

  test("import image (JPEG)") {
    val asset = importFile("images/1.jpg")
    val preview: Preview = altitude.service.library.getPreview(asset.id.get)
    preview.mimeType should equal("application/octet-stream")
    preview.data.length should not be 0
  }



  test("import file list") {
    val incomingPath = getClass.getResource("../import").getPath
    val assets = altitude.service.source.fileSystem.assetIterator(path=incomingPath)
    assets should not be empty
  }

  private def importFile(p: String): Asset = {
    val path = getClass.getResource(s"../import/$p").getPath
    val fileImportAsset = altitude.service.source.fileSystem.fileToImportAsset(new File(path))
    val importedAsset = altitude.service.assetImport.importAsset(fileImportAsset).get
    importedAsset.assetType should equal(importedAsset.assetType)
    importedAsset.path should not be empty
    importedAsset.md5 should not be empty

    val asset = altitude.service.library.getById(importedAsset.id.get): Asset
    asset.assetType should equal(importedAsset.assetType)
    asset.path should not be empty
    asset.md5 should not be empty
    asset.sizeBytes should not be 0
    asset
  }

}

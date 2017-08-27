package software.altitude.test.core.integration

import java.io.File

import org.scalatest.DoNotDiscover
import org.scalatest.Matchers._
import software.altitude.core.DuplicateException
import software.altitude.core.models.{Asset, Preview}

@DoNotDiscover class ImportTests(val config: Map[String, Any]) extends IntegrationTestCore {

  test("import duplicate") {
    importFile("images/1.jpg")
    val path = getClass.getResource(s"/import/images/1.jpg").getPath
    val importAsset = fileToImportAsset(new File(path))

    intercept[DuplicateException] {
      altitude.service.assetImport.importAsset(importAsset)
    }
  }

  test("import image") {
    val asset = importFile("images/1.jpg")
    val preview: Preview = altitude.service.library.getPreview(asset.id.get)
    preview.mimeType should equal("application/octet-stream")
    preview.data.length should not be 0
  }

  private def importFile(path: String): Asset = {
    val _path = getClass.getResource(s"/import/$path").getPath
    val fileImportAsset = fileToImportAsset(new File(_path))
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

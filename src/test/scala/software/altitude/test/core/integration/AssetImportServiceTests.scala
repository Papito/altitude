package software.altitude.test.core.integration

import org.scalatest.DoNotDiscover
import org.scalatest.matchers.must.Matchers.{be, empty, equal, not}
import org.scalatest.matchers.should.Matchers.convertToAnyShouldWrapper
import software.altitude.core.DuplicateException
import software.altitude.core.models.{Asset, ImportAsset, Preview}
import software.altitude.test.core.IntegrationTestCore
import software.altitude.test.core.IntegrationTestCore.fileToImportAsset

import java.io.File

@DoNotDiscover class AssetImportServiceTests(val config: Map[String, Any]) extends IntegrationTestCore {

  test("Import duplicate") {
    val importAsset = getImportAsset("images/2.jpg")
    altitude.service.assetImport.importAsset(importAsset).get

    intercept[DuplicateException] {
      altitude.service.assetImport.importAsset(importAsset).get
    }
  }

  test("Imported image should have all properties set") {
    val importAsset = getImportAsset("images/1.jpg")
    val importedAsset: Asset = altitude.service.assetImport.importAsset(importAsset).get

    importedAsset.assetType should equal(importedAsset.assetType)
    importedAsset.path should not be empty
    importedAsset.checksum should not be empty

    val asset = altitude.service.library.getById(importedAsset.id.get): Asset
    asset.assetType should equal(importedAsset.assetType)
    asset.path should not be empty
    asset.checksum should not be empty
    asset.sizeBytes should not be 0
  }

  test("Imported image should have a preview") {
    val importAsset = getImportAsset("images/1.jpg")
    val importedAsset: Asset = altitude.service.assetImport.importAsset(importAsset).get
    val asset = altitude.service.library.getById(importedAsset.id.get): Asset
    val preview: Preview = altitude.service.library.getPreview(asset.id.get)

    preview.mimeType should equal("application/octet-stream")
    preview.data.length should not be 0
  }

  test("Imported image is triaged") {
    val importAsset = getImportAsset("images/1.jpg")
    val importedAsset: Asset = altitude.service.assetImport.importAsset(importAsset).get
    val asset = altitude.service.library.getById(importedAsset.id.get): Asset
    asset.isTriaged should be(true)
  }

  private def getImportAsset(path: String): ImportAsset = {
    val _path = getClass.getResource(s"/import/$path").getPath
    val fileImportAsset = fileToImportAsset(new File(_path))
    fileImportAsset
  }

}

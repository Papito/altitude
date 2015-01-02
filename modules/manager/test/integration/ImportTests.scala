package integration

import java.io.File

import models.manager.FileImportAsset
import org.scalatest.Matchers._

class ImportTests extends IntegrationTests {
  test("import file list") {
    val incomingPath = getClass.getResource("../files/incoming").getPath
    val assets = global.ManagerGlobal.service.fileImport.getFilesToImport(path=incomingPath)
    assets should not be empty
  }

  test("detect image media type (JPEG)") {
    val path = getClass.getResource("../files/incoming/images/1.jpg").getPath
    val fileImportAsset = new FileImportAsset(new File(path))
    val assetType = global.ManagerGlobal.service.fileImport.detectAssetType(fileImportAsset)

    assetType.mediaType should equal ("image")
    assetType.mediaSubtype should equal ("jpeg")
    assetType.mime should equal ("image/jpeg")
  }

  test("detect audio media type (MP3)") {
    val path = getClass.getResource("../files/incoming/audio/all.mp3").getPath
    val fileImportAsset = new FileImportAsset(new File(path))
    val assetType = global.ManagerGlobal.service.fileImport.detectAssetType(fileImportAsset)

    assetType.mediaType should equal ("audio")
    assetType.mediaSubtype should equal ("mpeg")
    assetType.mime should equal ("audio/mpeg")
  }

  test("import image (JPEG)") {
    val path = getClass.getResource("../files/incoming/images/1.jpg").getPath
    val fileImportAsset = new FileImportAsset(new File(path))
    val asset = global.ManagerGlobal.service.fileImport.importAsset(fileImportAsset)
    whenReady(asset) {asset =>
      asset.getMessage() should equal("DatabaseException['empty lastError message']")
    }
  }

  test("import audio (MP3)") {
    val path = getClass.getResource("../files/incoming/audio/all.mp3").getPath
    val fileImportAsset = new FileImportAsset(new File(path))
    val asset = global.ManagerGlobal.service.fileImport.importAsset(fileImportAsset)
    whenReady(asset) {asset =>
      asset.getMessage() should equal("DatabaseException['empty lastError message']")
    }
  }
}

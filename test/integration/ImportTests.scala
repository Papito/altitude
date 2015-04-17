package integration

import java.io.File

import altitude.dao.TransactionId
import altitude.models.{Asset, FileImportAsset}
import org.scalatest.DoNotDiscover
import org.scalatest.Matchers._

import scala.concurrent.Future

@DoNotDiscover class ImportTests(val config: Map[String, _]) extends IntegrationTestCore {
  test("import image (JPEG)") {
    val path = getClass.getResource("../files/incoming/images/1.jpg").getPath
    val fileImportAsset = new FileImportAsset(new File(path))

    val importedAsset = altitude.service.fileImport.importAsset(fileImportAsset).futureValue
    val asset = altitude.service.library.getById(importedAsset.id.get).futureValue: Asset
    asset.mediaType should equal(importedAsset.mediaType)
  }

  test("import audio (MP3)") {
    val path = getClass.getResource("../files/incoming/audio/all.mp3").getPath
    val fileImportAsset = new FileImportAsset(new File(path))
    val importedAsset = altitude.service.fileImport.importAsset(fileImportAsset).futureValue
    val asset = altitude.service.library.getById(importedAsset.id.get).futureValue: Asset
    asset.mediaType should equal(importedAsset.mediaType)
    (asset.metadata \ "Author").as[String] should equal("Whitney Houston")
  }

  test("detect image media type (JPEG)") {
    val path = getClass.getResource("../files/incoming/images/1.jpg").getPath
    val fileImportAsset = new FileImportAsset(new File(path))
    val assetType = altitude.service.fileImport.detectAssetType(fileImportAsset)

    assetType.mediaType should equal ("image")
    assetType.mediaSubtype should equal ("jpeg")
    assetType.mime should equal ("image/jpeg")
  }

  test("detect audio media type (MP3)") {
    val path = getClass.getResource("../files/incoming/audio/all.mp3").getPath
    val fileImportAsset = new FileImportAsset(new File(path))
    val assetType = altitude.service.fileImport.detectAssetType(fileImportAsset)

    assetType.mediaType should equal ("audio")
    assetType.mediaSubtype should equal ("mpeg")
    assetType.mime should equal ("audio/mpeg")
  }

  test("import file list") {
    val incomingPath = getClass.getResource("../files/incoming").getPath
    val assets = altitude.service.fileImport.getFilesToImport(path=incomingPath)
    assets should not be empty
  }
}

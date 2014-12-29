package integration

import java.io.File

import models.manager.FileImportAsset
import org.scalatest._
import org.scalatest.concurrent.ScalaFutures
import org.scalatestplus.play._
import reactivemongo.core.commands.LastError

import scala.concurrent.Await
import scala.concurrent.duration._

class ImportTests extends FunSuite with OneAppPerSuite with ScalaFutures {
/*
  test("import file list") {
    val incomingPath = getClass.getResource("../files/incoming").getPath
    val assets = global.ManagerGlobal.importService.getFilesToImport(path=incomingPath)
    assets should not be empty
  }

  test("detect image media type (JPEG)") {
    val path = getClass.getResource("../files/incoming/images/1.jpg").getPath
    val fileImportAsset = new FileImportAsset(new File(path))
    val assetType = global.ManagerGlobal.importService.detectAssetType(fileImportAsset)

    val d = assetType.toMap
    d.valueAt("type") should equal("image")
    d.valueAt("subtype") should equal("jpeg")
    d.valueAt("mime") should equal("image/jpeg")
  }

  test("detect audio media type (MP3)") {
    val path = getClass.getResource("../files/incoming/audio/all.mp3").getPath
    val fileImportAsset = new FileImportAsset(new File(path))
    val assetType = global.ManagerGlobal.importService.detectAssetType(fileImportAsset)

    val d = assetType.toMap
    d.valueAt("type") should equal("audio")
    d.valueAt("subtype") should equal("mpeg")
    d.valueAt("mime") should equal("audio/mpeg")
  }
*/

  test("import image (JPEG)") {
    val path = getClass.getResource("../files/incoming/images/1.jpg").getPath
    val fileImportAsset = new FileImportAsset(new File(path))
    val asset = global.ManagerGlobal.service.fileImport.importAsset(fileImportAsset)
    Await.result(asset, 1.second)
  }

  test("import audio (MP3)") {
    val path = getClass.getResource("../files/incoming/audio/all.mp3").getPath
    val fileImportAsset = new FileImportAsset(new File(path))
    val asset = global.ManagerGlobal.service.fileImport.importAsset(fileImportAsset)
    Await.result(asset, 1.second)
  }
}

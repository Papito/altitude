package integration

import java.io.File

import models.ImportAsset
import org.scalatest._
import org.scalatestplus.play._
import org.scalatest.Matchers._
import org.scalatest.PartialFunctionValues._

class ImportTests extends FunSuite with OneAppPerSuite {
  test("import file list") {
    val incomingPath = getClass.getResource("../files/incoming").getPath
    val assets = global.ManagerGlobal.importService.getAssetsToImport(path=incomingPath)
    assets should not be empty
  }

  test("image media type (JPEG)") {
    val path = getClass.getResource("../files/incoming/images/1.jpg").getPath
    val importAsset = new ImportAsset(new File(path))
    val knownTypeImportAsset = global.ManagerGlobal.importService.getAssetWithType(importAsset)

    val d = knownTypeImportAsset.mediaType.toMap
    d.valueAt("type") should equal("image")
    d.valueAt("subtype") should equal("jpeg")
    d.valueAt("mime") should equal("image/jpeg")
  }
  test("audio media type (MP3)") {
    val path = getClass.getResource("../files/incoming/audio/all.mp3").getPath
    val importAsset = new ImportAsset(new File(path))
    val knownTypeImportAsset = global.ManagerGlobal.importService.getAssetWithType(importAsset)

    val d = knownTypeImportAsset.mediaType.toMap
    d.valueAt("type") should equal("audio")
    d.valueAt("subtype") should equal("mpeg")
    d.valueAt("mime") should equal("audio/mpeg")
  }
}

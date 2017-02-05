package integration

import java.io.File

import altitude.models.{Folder, Asset}
import altitude.util.Query
import org.scalatest.DoNotDiscover
import org.scalatest.Matchers._

@DoNotDiscover class FileStoreServiceTests(val config: Map[String, Any]) extends IntegrationTestCore {

  test("folder management") {
    /*
      folder2
        folder2_1
          folder2_1_1
        folder2_2
      */
/*
    val folder2: Folder = altitude.service.folder.addFolder(
      name = "folder2")

    folder2.parentId shouldEqual ctx.repo.rootFolderId

    val folder2_1: Folder = altitude.service.folder.addFolder(
      name = "folder2_1", parentId = folder2.id)

    folder2.id should contain(folder2_1.parentId)

    val folder2_1_1: Folder = altitude.service.folder.addFolder(
      name = "folder2_1_1", parentId = folder2_1.id)

    folder2_1.id should contain(folder2_1_1.parentId)

    val folder2_2: Folder = altitude.service.folder.addFolder(
      name = "folder2_2", parentId = folder2.id)

*/
  }

/*
  test("import file list") {
    val asset = importFile("images/1.jpg")
    val results = altitude.service.library.query(Query())
    results.records.length shouldBe 1
  }
*/

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

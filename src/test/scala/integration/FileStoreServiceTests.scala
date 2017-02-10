package integration

import java.io.File

import altitude.exceptions.{NotFoundException, IllegalOperationException}
import altitude.models.{Folder, Asset}
import altitude.{Const => C}
import org.apache.commons.io.FilenameUtils
import org.scalatest.DoNotDiscover
import org.scalatest.Matchers._

@DoNotDiscover class FileStoreServiceTests(val config: Map[String, Any]) extends IntegrationTestCore {

  test("folder management") {
    /*
      folder1
      folder2
        folder2_1
          folder2_1_1
        folder2_2
      */

    val folder1: Folder = altitude.service.folder.addFolder(
      name = "folder1")

    var relativePath = FilenameUtils.concat(C.Path.SORTED, folder1.name)
    checkRepositoryDirPath(relativePath)

    val folder2: Folder = altitude.service.folder.addFolder(
      name = "folder2")

    relativePath = FilenameUtils.concat(C.Path.SORTED, folder2.name)
    checkRepositoryDirPath(relativePath)

    val folder2_1: Folder = altitude.service.folder.addFolder(
      name = "folder2_1", parentId = folder2.id)

    relativePath = FilenameUtils.concat(C.Path.SORTED, folder2_1.path)
    checkRepositoryDirPath(relativePath)

    val folder2_1_1: Folder = altitude.service.folder.addFolder(
      name = "folder2_1_1", parentId = folder2_1.id)

    relativePath = FilenameUtils.concat(C.Path.SORTED, folder2_1_1.path)
    checkRepositoryDirPath(relativePath)

    val folder2_2: Folder = altitude.service.folder.addFolder(
      name = "folder2_2", parentId = folder2.id)

    relativePath = FilenameUtils.concat(C.Path.SORTED, folder2_2.path)
    checkRepositoryDirPath(relativePath)

    // delete the empty folder
    altitude.service.folder.deleteById(folder1.id.get)
    relativePath = FilenameUtils.concat(C.Path.SORTED, folder1.name)
    checkNoRepositoryDirPath(relativePath)

    // delete the folder with children
    altitude.service.folder.deleteById(folder2.id.get)
    relativePath = FilenameUtils.concat(C.Path.SORTED, folder2.name)
    checkNoRepositoryDirPath(relativePath)

    // doing so again is a not found
    intercept[NotFoundException] {
        altitude.service.folder.deleteById(folder2.id.get)
    }

    val folder3: Folder = altitude.service.folder.addFolder(
      name = "folder3")

    val folder3_3: Folder = altitude.service.folder.addFolder(
      name = "folder3_3", parentId = folder3.id)

    altitude.service.folder.rename(folder3.id.get, "folder3_new")
    relativePath = FilenameUtils.concat(C.Path.SORTED, folder3_3.name)
    checkNoRepositoryDirPath(relativePath)
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

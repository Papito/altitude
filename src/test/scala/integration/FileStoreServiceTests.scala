package integration

import java.io.File

import altitude.models.{Folder, Asset}
import altitude.{Const => C, NotFoundException}
import org.apache.commons.io.FilenameUtils
import org.scalatest.DoNotDiscover
import org.scalatest.Matchers._

@DoNotDiscover class FileStoreServiceTests(val config: Map[String, Any]) extends IntegrationTestCore {

  test("folders") {
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

    relativePath = folder2_1.path
    checkRepositoryDirPath(relativePath)

    val folder2_1_1: Folder = altitude.service.folder.addFolder(
      name = "folder2_1_1", parentId = folder2_1.id)

    relativePath = folder2_1_1.path
    checkRepositoryDirPath(relativePath)

    val folder2_2: Folder = altitude.service.folder.addFolder(
      name = "folder2_2", parentId = folder2.id)

    relativePath = folder2_2.path
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

  test("restore asset") {
    val asset = importFile("images/1.jpg")
    val relAssetPath = new File(altitude.service.fileStore.triageFolderPath, "1.jpg")
    checkRepositoryFilePath(relAssetPath.getPath)

    // move asset to trash
    altitude.service.library.recycleAsset(asset.id.get)
    checkNoRepositoryFilePath(relAssetPath.getPath)

    // restore the asset
    altitude.service.library.restoreRecycledAsset(asset.id.get)
    checkRepositoryFilePath(relAssetPath.getPath)
  }

  test("move asset") {
    val asset = importFile("images/1.jpg")
    var relAssetPath = new File(altitude.service.fileStore.triageFolderPath, "1.jpg")
    checkRepositoryFilePath(relAssetPath.getPath)

    val folder: Folder = altitude.service.folder.addFolder("folder1")
    relAssetPath = new File(folder.path, "1.jpg")
    val movedAsset = altitude.service.library.moveAssetToFolder(asset.id.get, folder.id.get)
    checkRepositoryFilePath(relAssetPath.getPath)
    movedAsset.path shouldBe relAssetPath.getPath
  }

  private def importFile(path: String): Asset = {
    val _path = getClass.getResource(s"../import/$path").getPath
    val fileImportAsset = altitude.service.source.fileSystem.fileToImportAsset(new File(_path))
    val importedAsset = altitude.service.assetImport.importAsset(fileImportAsset).get
    importedAsset.assetType should equal(importedAsset.assetType)
    importedAsset.path should not be empty
    importedAsset.md5 should not be empty

    val asset: Asset = altitude.service.library.getById(importedAsset.id.get)
    asset.assetType should equal(importedAsset.assetType)
    asset.path should not be empty
    asset.md5 should not be empty
    asset.sizeBytes should not be 0
    asset
  }

}

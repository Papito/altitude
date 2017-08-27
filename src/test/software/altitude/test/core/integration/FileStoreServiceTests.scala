package software.altitude.test.core.integration

import java.io.File

import org.apache.commons.io.{FileUtils, FilenameUtils}
import org.scalatest.DoNotDiscover
import org.scalatest.Matchers._
import software.altitude.core.models.{Asset, Folder}
import software.altitude.core.{Const => C, NotFoundException, StorageException}

@DoNotDiscover class FileStoreServiceTests(val config: Map[String, Any]) extends IntegrationTestCore {

/*
  test("move asset") {
    val asset = importFile("images/1.jpg")
    var relAssetPath = new File(altitude.service.fileStore.triageFolderPath, "1.jpg")
    checkRepositoryFilePath(relAssetPath.getPath)

    var folder: Folder = altitude.service.folder.addFolder("folder1")
    relAssetPath = new File(folder.path.get, "1.jpg")
    var movedAsset = altitude.service.library.moveAssetToFolder(asset.id.get, folder.id.get)
    checkRepositoryFilePath(relAssetPath.getPath)
    movedAsset.path contains relAssetPath.getPath
  }

  test("move folder") {
    var folder1: Folder = altitude.service.folder.addFolder("folder1")
    var asset1: Asset = altitude.service.library.add(makeAsset(folder1))
    val folder2: Folder = altitude.service.folder.addFolder("folder2")
    val asset2: Asset = altitude.service.library.add(makeAsset(folder2))

    altitude.service.folder.move(folder1.id.get, folder2.id.get)
    checkNoRepositoryDirPath(folder1.path.get)

    folder1 = altitude.service.folder.getById(folder1.id.get)
    checkRepositoryDirPath(folder1.path.get)

    asset1 = altitude.app.service.library.getById(asset1.id.get)
    checkRepositoryFilePath(asset1.path.get)
  }

  test("move asset into conflicting file") {
    val asset = importFile("images/1.jpg")
    var relAssetPath = new File(altitude.service.fileStore.triageFolderPath, "1.jpg")
    checkRepositoryFilePath(relAssetPath.getPath)

    var folder: Folder = altitude.service.folder.addFolder("folder1")
    relAssetPath = new File(folder.path.get, "1.jpg")
    val movedAsset = altitude.service.library.moveAssetToFolder(asset.id.get, folder.id.get)

    // create a dummy file in place of destination
    folder  = altitude.service.folder.addFolder("folder2")
    relAssetPath = new File(folder.path.get, "1.jpg")
    FileUtils.writeByteArrayToFile(getAbsoluteFile(relAssetPath.getPath), new Array[Byte](0))

    // move the file again - into an existing file
    intercept[StorageException] {
      altitude.service.library.moveAssetToFolder(movedAsset.id.get, folder.id.get)
    }
  }

  test("rename directory") {
    var folder1: Folder = altitude.service.folder.addFolder("folder1")

    var asset1: Asset = altitude.service.library.add(makeAsset(folder1))
    val asset2: Asset = altitude.service.library.add(makeAsset(folder1))

    asset1.path should not be None
    asset2.path should not be None

    checkRepositoryFilePath(asset1.path.get)
    checkRepositoryFilePath(asset2.path.get)

    folder1 = altitude.service.library.renameFolder(folder1.id.get, "newName")

    checkNoRepositoryFilePath(asset1.path.get)
    checkNoRepositoryFilePath(asset2.path.get)

    asset1 = altitude.service.library.getById(asset1.id.get)
    asset1.path should not be None
    asset1.path.get should not be empty
    asset1.path contains "newName"
    asset1.path.get.endsWith(asset1.fileName) shouldBe true
    checkRepositoryFilePath(asset1.path.get)
  }

  test("delete folder") {
    val folder1: Folder = altitude.service.folder.addFolder("folder1")
    altitude.service.library.deleteFolderById(folder1.id.get)
    checkRepositoryDirPath("")
  }

  test("delete folder and assets") {
    val folder1: Folder = altitude.service.folder.addFolder("folder1")

    val asset1: Asset = altitude.service.library.add(makeAsset(folder1))
    val asset2: Asset = altitude.service.library.add(makeAsset(folder1))

    asset1.path should not be None
    asset2.path should not be None

    checkRepositoryFilePath(asset1.path.get)
    checkRepositoryFilePath(asset2.path.get)

    altitude.service.library.deleteFolderById(folder1.id.get)

    checkNoRepositoryFilePath(asset1.path.get)
    checkNoRepositoryFilePath(asset2.path.get)
  }

  test("restore asset") {
    val asset1 = importFile("images/1.jpg")
    val relAssetPath = new File(altitude.service.fileStore.triageFolderPath, "1.jpg")
    checkRepositoryFilePath(relAssetPath.getPath)

    // move asset to trash
    altitude.service.library.recycleAsset(asset1.id.get)
    checkNoRepositoryFilePath(relAssetPath.getPath)

    // restore the asset
    altitude.service.library.restoreRecycledAsset(asset1.id.get)
    checkRepositoryFilePath(relAssetPath.getPath)
  }

  test("restore asset into conflicting file") {
    val asset1 = importFile("images/1.jpg")
    val relAssetPath = new File(altitude.service.fileStore.triageFolderPath, "1.jpg")
    checkRepositoryFilePath(relAssetPath.getPath)

    // move asset to trash
    altitude.service.library.recycleAsset(asset1.id.get)
    checkNoRepositoryFilePath(relAssetPath.getPath)

    // create a dummy file in place of previously recycled asset
    FileUtils.writeByteArrayToFile(getAbsoluteFile(asset1.path.get), new Array[Byte](0))
    checkRepositoryFilePath(relAssetPath.getPath)

    intercept[StorageException] {
      altitude.service.library.restoreRecycledAsset(asset1.id.get)
    }
  }

  test("rename asset") {
    var asset = importFile("images/1.jpg")
    var relAssetPath = new File(altitude.service.fileStore.triageFolderPath, "1.jpg")
    checkRepositoryFilePath(relAssetPath.getPath)

    asset = altitude.service.library.renameAsset(asset.id.get, "2.jpg")
    relAssetPath = new File(altitude.service.fileStore.triageFolderPath, "2.jpg")
    checkRepositoryFilePath(relAssetPath.getPath)

    asset = altitude.service.library.renameAsset(asset.id.get, "3.jpg")
    relAssetPath = new File(altitude.service.fileStore.triageFolderPath, "3.jpg")
    checkRepositoryFilePath(relAssetPath.getPath)
  }

  test("rename asset into existing file") {
    var asset = importFile("images/1.jpg")
    var relAssetPath = new File(altitude.service.fileStore.triageFolderPath, "1.jpg")
    checkRepositoryFilePath(relAssetPath.getPath)

    asset = altitude.service.library.renameAsset(asset.id.get, "2.jpg")
    relAssetPath = new File(altitude.service.fileStore.triageFolderPath, "2.jpg")
    checkRepositoryFilePath(relAssetPath.getPath)

    // rename the asset with a file already at destination
    relAssetPath = new File(altitude.service.fileStore.triageFolderPath, "3.jpg")
    FileUtils.writeByteArrayToFile(getAbsoluteFile(relAssetPath.getPath), new Array[Byte](0))

    intercept[StorageException] {
      asset = altitude.service.library.renameAsset(asset.id.get, "3.jpg")
    }
  }

  test("manage folders") {
    /*
      folder1
      folder2
        folder2_1
          folder2_1_1
        folder2_2
      */

    val folder1: Folder = altitude.service.folder.addFolder(
      name = "folder1")

    var relativePath = FilenameUtils.concat(C.Path.ROOT, folder1.name)
    checkRepositoryDirPath(relativePath)

    val folder2: Folder = altitude.service.folder.addFolder(
      name = "folder2")

    relativePath = FilenameUtils.concat(C.Path.ROOT, folder2.name)
    checkRepositoryDirPath(relativePath)

    val folder2_1: Folder = altitude.service.folder.addFolder(
      name = "folder2_1", parentId = folder2.id)

    relativePath = folder2_1.path.get
    checkRepositoryDirPath(relativePath)

    val folder2_1_1: Folder = altitude.service.folder.addFolder(
      name = "folder2_1_1", parentId = folder2_1.id)

    relativePath = folder2_1_1.path.get
    checkRepositoryDirPath(relativePath)

    val folder2_2: Folder = altitude.service.folder.addFolder(
      name = "folder2_2", parentId = folder2.id)

    relativePath = folder2_2.path.get
    checkRepositoryDirPath(relativePath)

    // delete the empty folder
    altitude.service.library.deleteFolderById(folder1.id.get)
    relativePath = FilenameUtils.concat(C.Path.ROOT, folder1.name)
    checkNoRepositoryDirPath(relativePath)

    // delete the folder with children
    altitude.service.library.deleteFolderById(folder2.id.get)
    relativePath = FilenameUtils.concat(C.Path.ROOT, folder2.name)
    checkNoRepositoryDirPath(relativePath)

    // doing so again is a not found
    intercept[NotFoundException] {
        altitude.service.library.deleteFolderById(folder2.id.get)
    }

    val folder3: Folder = altitude.service.folder.addFolder(
      name = "folder3")

    val folder3_3: Folder = altitude.service.folder.addFolder(
      name = "folder3_3", parentId = folder3.id)

    altitude.service.folder.rename(folder3.id.get, "folder3_new")
    relativePath = FilenameUtils.concat(C.Path.ROOT, folder3_3.name)
    checkNoRepositoryDirPath(relativePath)
    relativePath = FilenameUtils.concat(C.Path.ROOT, "folder3_new")
    checkRepositoryDirPath(relativePath)
  }

  private def checkRepositoryDirPath(path: String) = {
    // get current repo root
    val f = getAbsoluteFile(path)
    f.exists shouldBe true
    f.isDirectory shouldBe true
  }

  private def checkNoRepositoryDirPath(path: String) = {
    val f = getAbsoluteFile(path)
    f.exists shouldBe false
  }

  private def checkRepositoryFilePath(path: String) = {
    val f = getAbsoluteFile(path)
    f.exists shouldBe true
    f.isFile shouldBe true
  }

  private def checkNoRepositoryFilePath(path: String) = {
    // get current repo root
    val rootPath = currentRepo.fileStoreConfig(C.Repository.Config.PATH)
    val f = new File(rootPath, path)
    f.exists shouldBe false
  }

  private def getAbsoluteFile(path: String): File = {
    val rootPath = currentRepo.fileStoreConfig(C.Repository.Config.PATH)
    new File(rootPath, path)
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
*/
}

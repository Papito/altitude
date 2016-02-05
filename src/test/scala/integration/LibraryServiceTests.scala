package integration

import altitude.{Const => C, Util}
import altitude.models.search.Query
import altitude.models.{Asset, MediaType, Folder}
import org.scalatest.Matchers._

class LibraryServiceTests (val config: Map[String, String]) extends IntegrationTestCore {

  test("move asset to a different folder") {
    /*
    folder1
    folder2
    */
    val folder1: Folder = altitude.service.folder.add(
      Folder(name = "folder1"))

    val folder2: Folder = altitude.service.folder.add(
      Folder(name = "folder2"))

    (altitude.service.folder.getById(folder1.id.get): Folder).numOfAssets should be (0)

    // uncategorized folder should be empty
    val systemFolders = altitude.service.folder.getSystemFolders
    systemFolders(Folder.UNCATEGORIZED.id.get).numOfAssets should be (0)
    systemFolders(Folder.TRASH.id.get).numOfAssets should be (0)

    val mediaType = new MediaType(mediaType = "mediaType", mediaSubtype = "mediaSubtype", mime = "mime")

    val asset: Asset = altitude.service.library.add(
      new Asset(
        folderId = folder1.id.get.toString,
        mediaType = mediaType,
        path = Util.randomStr(30),
        md5 = Util.randomStr(30),
        sizeBytes = 1L))

    val asset2: Asset = altitude.service.library.add(
      new Asset(
        folderId = Folder.UNCATEGORIZED.id.get,
        mediaType = mediaType,
        path = Util.randomStr(30),
        md5 = Util.randomStr(30),
        sizeBytes = 1L))

    // there is an uncategorized asset
    val systemFolders2 = altitude.service.folder.getSystemFolders
    systemFolders2(Folder.UNCATEGORIZED.id.get).numOfAssets should be (1)

    altitude.service.library.search(
      Query(params = Map(C.Api.Folder.QUERY_ARG_NAME -> folder1.id.get))
    ).length should be(1)
    (altitude.service.folder.getById(folder1.id.get): Folder).numOfAssets should be (1)

    altitude.service.library.moveToFolder(asset.id.get, folder2.id.get)

    altitude.service.library.search(
      Query(params = Map(C.Api.Folder.QUERY_ARG_NAME -> folder1.id.get))
    ).length should be(0)
    (altitude.service.folder.getById(folder1.id.get): Folder).numOfAssets should be (0)

    altitude.service.library.search(
      Query(params = Map(C.Api.Folder.QUERY_ARG_NAME -> folder2.id.get))
    ).length should be(1)
    (altitude.service.folder.getById(folder2.id.get): Folder).numOfAssets should be (1)

    altitude.service.library.moveToFolder(asset2.id.get, folder1.id.get)
    altitude.service.library.moveToFolder(asset.id.get, Folder.TRASH.id.get)

    val systemFolders3 = altitude.service.folder.getSystemFolders
    systemFolders3(Folder.UNCATEGORIZED.id.get).numOfAssets should be (0)
    systemFolders3(Folder.TRASH.id.get).numOfAssets should be (1)
  }

}

package integration

import altitude.exceptions.NotFoundException
import altitude.models.search.Query
import altitude.models.{Trash, Asset, Folder}
import altitude.{Const => C, Util}
import org.scalatest.DoNotDiscover
import org.scalatest.Matchers._
import play.api.libs.json.JsObject

@DoNotDiscover class LibraryServiceTests (val config: Map[String, String]) extends IntegrationTestCore {

  test("recycle asset") {
    altitude.service.library.add(makeAsset(Folder.UNCATEGORIZED))
    altitude.service.asset.getAll.length should be (1)

    val asset: Asset = altitude.service.asset.getAll.head
    altitude.service.library.recycleAsset(asset.id.get)

    altitude.service.asset.getAll.length should be (0)
    altitude.service.trash.getAll.length should be (1)

    val trashed: Trash = altitude.service.trash.getAll.head
    trashed.createdAt should not be None
    trashed.createdAt.get.getMillis should equal(asset.createdAt.get.getMillis)
    trashed.recycledAt should not be None
  }

  test("move asset to a different folder") {
    /*
    folder1
    folder2
    */
    val folder1: Folder = altitude.service.folder.add(
      Folder(name = "folder1"))

    val folder2: Folder = altitude.service.folder.add(
      Folder(name = "folder2"))

    val asset: Asset = altitude.service.library.add(makeAsset(folder1))

    altitude.service.library.search(
      Query(USER, params = Map(C("Api.Folder.QUERY_ARG_NAME") -> folder1.id.get))
    ).records.length should be(1)

    altitude.service.library.moveAssetToFolder(asset.id.get, folder2.id.get)

    altitude.service.library.search(
      Query(USER, params = Map(C("Api.Folder.QUERY_ARG_NAME") -> folder1.id.get))
    ).records.length should be(0)

    altitude.service.library.search(
      Query(USER, params = Map(C("Api.Folder.QUERY_ARG_NAME") -> folder2.id.get))
    ).records.length should be(1)
  }

  test("folder counts") {
    /*
    folder1
    folder2
      folder2_1
      folder2_2
        folder2_2_1
        folder2_2_2
    */
    val folder1: Folder = altitude.service.folder.add(
      Folder(name = "folder1"))

    val folder2: Folder = altitude.service.folder.add(
      Folder(name = "folder2"))

    val folder2_1: Folder = altitude.service.folder.add(
      Folder(name = "folder2_1", parentId = folder2.id.get))

    val folder2_2: Folder = altitude.service.folder.add(
      Folder(name = "folder2_2", parentId = folder2.id.get))

    val folder2_2_1: Folder = altitude.service.folder.add(
      Folder(name = "folder2_2_1", parentId = folder2_2.id.get))

    val folder2_2_2: Folder = altitude.service.folder.add(
      Folder(name = "folder2_2_2", parentId = folder2_2.id.get))

    // fill up the hierarchy with assets x times over
    1 to 2 foreach {n =>
      altitude.service.library.add(makeAsset(Folder.UNCATEGORIZED))
      altitude.service.library.add(makeAsset(folder1))
      altitude.service.library.add(makeAsset(folder2))
      altitude.service.library.add(makeAsset(folder2_1))
      altitude.service.library.add(makeAsset(folder2_2))
      altitude.service.library.add(makeAsset(folder2_2_1))
      altitude.service.library.add(makeAsset(folder2_2_2))
    }

    // check counts
    val systemFolders = altitude.service.folder.getSysFolders()
    systemFolders(Folder.UNCATEGORIZED.id.get).numOfAssets should be (2)

    // prefetch all folders for speed
    val all = altitude.service.folder.getNonSysFolders()

    // test counts for individual folders
    (altitude.service.folder.getByIdWithChildAssetCounts(folder1.id.get, all): Folder).numOfAssets should be (2)
    (altitude.service.folder.getByIdWithChildAssetCounts(folder2_2_1.id.get, all): Folder).numOfAssets should be (2)
    (altitude.service.folder.getByIdWithChildAssetCounts(folder2_2_2.id.get, all): Folder).numOfAssets should be (2)
    (altitude.service.folder.getByIdWithChildAssetCounts(folder2_2.id.get, all): Folder).numOfAssets should be (6)
    (altitude.service.folder.getByIdWithChildAssetCounts(folder2_1.id.get, all): Folder).numOfAssets should be (2)
    (altitude.service.folder.getByIdWithChildAssetCounts(folder2.id.get, all): Folder).numOfAssets should be (10)

    // test counts for immediate children
    val rootChildren = altitude.service.folder.immediateChildren(Folder.ROOT.id.get, all)
    rootChildren.head.numOfAssets should be(2)
    rootChildren.last.numOfAssets should be(10)

    val rootChildren2 = altitude.service.folder.immediateChildren(Folder.ROOT.id.get)
    rootChildren2.head.numOfAssets should be(2)
    rootChildren2.last.numOfAssets should be(10)

    // test counts for hierarchy
    val hierarchy = altitude.service.folder.hierarchy()
    hierarchy.head.numOfAssets should be(2)
    hierarchy.last.numOfAssets should be(10)
  }

  test("move recyclded asset to folder") {
    val asset: Asset = altitude.service.library.add(makeAsset(Folder.UNCATEGORIZED))
    altitude.service.library.recycleAsset(asset.id.get)

    val folder1: Folder = altitude.service.folder.add(
      Folder(name = "folder1"))

    altitude.service.library.moveRecycledAssetToFolder(asset.id.get, folder1.id.get)
    altitude.service.trash.getAll.length should be (0)
    altitude.service.asset.getAll.length should be (1)

    altitude.service.library.search(
      Query(USER, params = Map(C("Api.Folder.QUERY_ARG_NAME") -> folder1.id.get))
    ).records.length should be(1)

    val all = altitude.service.folder.getNonSysFolders()

    (altitude.service.folder.getByIdWithChildAssetCounts(folder1.id.get, all): Folder).numOfAssets should be (1)

  }

  test("restore recycled asset") {
    val asset: Asset = altitude.service.library.add(makeAsset(Folder.UNCATEGORIZED))
    val trashed: Trash = altitude.service.library.recycleAsset(asset.id.get)
    altitude.service.library.restoreRecycledAsset(trashed.id.get)
    altitude.service.trash.getAll.length should be (0)
    altitude.service.asset.getAll.length should be (1)
  }

  test("restore recycled asset to non-existing folder") {
    val asset: Asset = altitude.service.library.add(makeAsset(Folder.UNCATEGORIZED))
    altitude.service.library.recycleAsset(asset.id.get)

    intercept[NotFoundException] {
      altitude.service.library.moveRecycledAssetToFolder(asset.id.get, "bad")
    }
  }
}

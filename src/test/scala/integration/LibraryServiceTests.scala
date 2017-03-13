package integration

import altitude.models.{Asset, Folder}
import altitude.util.Query
import altitude.{Const => C, IllegalOperationException, NotFoundException}
import org.scalatest.DoNotDiscover
import org.scalatest.Matchers._

@DoNotDiscover class LibraryServiceTests(val config: Map[String, Any]) extends IntegrationTestCore {

  test("rename asset") {
    var asset: Asset = altitude.service.library.add(makeAsset(altitude.service.folder.getTriageFolder))
    var updatedAsset: Asset = altitude.service.library.renameAsset(asset.id.get, "newName")
    updatedAsset.fileName shouldBe "newName"
    updatedAsset.path.contains("newName") shouldBe true

    // get the asset again to make sure it has been updated
    updatedAsset = altitude.service.library.getById(asset.id.get)
    updatedAsset.fileName shouldBe "newName"
    updatedAsset.path.contains("newName") shouldBe true

    // attempt to rename a recycled asset
    asset = altitude.service.library.recycleAsset(asset.id.get)

    intercept[IllegalOperationException] {
      altitude.service.library.renameAsset(asset.id.get, "newName2")
    }
  }

  test("move recycled asset to folder") {
    val asset: Asset = altitude.service.library.add(makeAsset(altitude.service.folder.getTriageFolder))
    altitude.service.asset.query(Query()).records.length shouldBe 1
    altitude.service.asset.queryRecycled(Query()).records.length shouldBe 0
    altitude.service.library.recycleAsset(asset.id.get)
    altitude.service.asset.queryRecycled(Query()).records.length shouldBe 1

    val folder1: Folder = altitude.service.folder.addFolder("folder1")

    altitude.service.library.moveAssetToFolder(asset.id.get, folder1.id.get)
    altitude.service.asset.queryRecycled(Query()).records.length shouldBe 0
    altitude.service.asset.query(Query()).records.length shouldBe 1

    altitude.service.library.query(
      Query(params = Map(C.Api.Folder.QUERY_ARG_NAME -> folder1.id.get))
    ).records.length shouldBe 1

    val all = altitude.service.folder.getUserFolders()

    (altitude.service.folder.getByIdWithChildAssetCounts(folder1.id.get, all): Folder).numOfAssets should be (1)
  }


  test("move asset to a different folder") {
    /*
    folder1
    folder2
    */
    val folder1: Folder = altitude.service.folder.addFolder("folder1")

    val folder2: Folder = altitude.service.folder.addFolder("folder2")

    val asset: Asset = altitude.service.library.add(makeAsset(folder1))

    altitude.service.library.query(
      Query(params = Map(C.Api.Folder.QUERY_ARG_NAME -> folder1.id.get))
    ).records.length shouldBe 1

    altitude.service.library.moveAssetToFolder(asset.id.get, folder2.id.get)

    altitude.service.library.query(
      Query(params = Map(C.Api.Folder.QUERY_ARG_NAME -> folder1.id.get))
    ).records.length shouldBe 0

    altitude.service.library.query(
      Query(params = Map(C.Api.Folder.QUERY_ARG_NAME -> folder2.id.get))
    ).records.length shouldBe 1

    SET_SECONDARY_REPO()

    altitude.service.library.query(Query()).isEmpty shouldBe true

    altitude.service.library.query(
      Query(params = Map(C.Api.Folder.QUERY_ARG_NAME -> folder1.id.get))
    ).isEmpty shouldBe true
  }

  test("recycle asset") {
    altitude.service.library.add(makeAsset(altitude.service.folder.getTriageFolder))

    SET_SECONDARY_USER()
    altitude.service.library.add(makeAsset(altitude.service.folder.getTriageFolder))

    SET_PRIMARY_USER()
    altitude.service.asset.query(Query()).records.length shouldBe 2

    val asset: Asset = altitude.service.asset.query(Query()).records.head
    altitude.service.library.recycleAsset(asset.id.get)

    altitude.service.asset.query(Query()).records.length shouldBe 1
    altitude.service.asset.queryRecycled(Query()).records.length shouldBe 1

    SET_SECONDARY_REPO()

    altitude.service.asset.queryRecycled(Query()).records.length shouldBe 0
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
    val folder1: Folder = altitude.service.folder.addFolder("folder1")

    val folder2: Folder = altitude.service.folder.addFolder("folder2")

    val folder2_1: Folder = altitude.service.folder.addFolder(
      name = "folder2_1", parentId = folder2.id)

    val folder2_2: Folder = altitude.service.folder.addFolder(
      name = "folder2_2", parentId = folder2.id)

    val folder2_2_1: Folder = altitude.service.folder.addFolder(
      name = "folder2_2_1", parentId = folder2_2.id)

    val folder2_2_2: Folder = altitude.service.folder.addFolder(
      name = "folder2_2_2", parentId = folder2_2.id)

    // fill up the hierarchy with assets x times over
    1 to 2 foreach {n =>
      altitude.service.library.add(makeAsset(altitude.service.folder.getTriageFolder))
      altitude.service.library.add(makeAsset(folder1))
      altitude.service.library.add(makeAsset(folder2))
      altitude.service.library.add(makeAsset(folder2_1))
      altitude.service.library.add(makeAsset(folder2_2))
      altitude.service.library.add(makeAsset(folder2_2_1))
      altitude.service.library.add(makeAsset(folder2_2_2))
    }

    // check counts
    val systemFolders = altitude.service.folder.getSysFolders()
    systemFolders(ctx.repo.triageFolderId).numOfAssets should be (2)

    // prefetch all folders for speed
    val all = altitude.service.folder.getUserFolders()

    // test counts for individual folders
    (altitude.service.folder.getByIdWithChildAssetCounts(folder1.id.get, all): Folder).numOfAssets should be (2)
    (altitude.service.folder.getByIdWithChildAssetCounts(folder2_2_1.id.get, all): Folder).numOfAssets should be (2)
    (altitude.service.folder.getByIdWithChildAssetCounts(folder2_2_2.id.get, all): Folder).numOfAssets should be (2)
    (altitude.service.folder.getByIdWithChildAssetCounts(folder2_2.id.get, all): Folder).numOfAssets should be (6)
    (altitude.service.folder.getByIdWithChildAssetCounts(folder2_1.id.get, all): Folder).numOfAssets should be (2)
    (altitude.service.folder.getByIdWithChildAssetCounts(folder2.id.get, all): Folder).numOfAssets should be (10)

    // test counts for immediate children
    val rootChildren = altitude.service.folder.immediateChildren(ctx.repo.rootFolderId, all)
    rootChildren.head.numOfAssets shouldBe 2
    rootChildren.last.numOfAssets shouldBe 10

    val rootChildren2 = altitude.service.folder.immediateChildren(ctx.repo.rootFolderId)
    rootChildren2.head.numOfAssets shouldBe 2
    rootChildren2.last.numOfAssets shouldBe 10

    // test counts for hierarchy
    val hierarchy = altitude.service.folder.hierarchy()
    hierarchy.head.numOfAssets shouldBe 2
    hierarchy.last.numOfAssets shouldBe 10
  }

  test("get recycled asset") {
    val asset: Asset = altitude.service.library.add(makeAsset(altitude.service.folder.getTriageFolder))
    altitude.service.library.recycleAsset(asset.id.get)
  }

  test("restore recycled asset") {
    val asset: Asset = altitude.service.library.add(makeAsset(altitude.service.folder.getTriageFolder))
    val trashed: Asset = altitude.service.library.recycleAsset(asset.id.get)
    altitude.service.library.restoreRecycledAsset(trashed.id.get)
    altitude.service.asset.query(Query()).isEmpty shouldBe false
  }

  test("restore recycled asset to non-existing folder") {
    val asset: Asset = altitude.service.library.add(makeAsset(altitude.service.folder.getTriageFolder))
    altitude.service.library.recycleAsset(asset.id.get)

    intercept[NotFoundException] {
      altitude.service.library.moveAssetToFolder(asset.id.get, "bad")
    }
  }
}

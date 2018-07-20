package software.altitude.test.core.integration

import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito
import org.mockito.invocation.InvocationOnMock
import org.scalatest.DoNotDiscover
import org.scalatest.Matchers._
import software.altitude.core.models._
import software.altitude.core.util.Query
import software.altitude.core.{DuplicateException, IllegalOperationException, NotFoundException, StorageException, Const => C}

@DoNotDiscover class LibraryServiceTests(val config: Map[String, Any]) extends IntegrationTestCore {

  test("Folder counts should check out") {
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
      altitude.service.library.add(makeAsset(altitude.service.folder.triageFolder))
      altitude.service.library.add(makeAsset(folder1))
      altitude.service.library.add(makeAsset(folder2))
      altitude.service.library.add(makeAsset(folder2_1))
      altitude.service.library.add(makeAsset(folder2_2))
      altitude.service.library.add(makeAsset(folder2_2_1))
      altitude.service.library.add(makeAsset(folder2_2_2))
    }

    // check counts
    val systemFolders = altitude.service.folder.sysFoldersByIdMap()

    // we do not increment triage folder - this is recorded in Stats
    systemFolders(ctx.repo.triageFolderId).numOfAssets shouldBe 0

    // prefetch all folders for speed
    val all = altitude.service.folder.repositoryFolders()

    // test counts for individual folders
    (altitude.service.folder.getByIdWithChildAssetCounts(folder1.id.get, all): Folder).numOfAssets shouldBe 2
    (altitude.service.folder.getByIdWithChildAssetCounts(folder2_2_1.id.get, all): Folder).numOfAssets shouldBe 2
    (altitude.service.folder.getByIdWithChildAssetCounts(folder2_2_2.id.get, all): Folder).numOfAssets shouldBe 2
    (altitude.service.folder.getByIdWithChildAssetCounts(folder2_2.id.get, all): Folder).numOfAssets shouldBe 6
    (altitude.service.folder.getByIdWithChildAssetCounts(folder2_1.id.get, all): Folder).numOfAssets shouldBe 2
    (altitude.service.folder.getByIdWithChildAssetCounts(folder2.id.get, all): Folder).numOfAssets shouldBe 10

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

  test("Rename asset and attempt to rename a recycled asset") {
    var asset: Asset = altitude.service.library.add(makeAsset(altitude.service.folder.triageFolder))
    var updatedAsset: Asset = altitude.service.library.renameAsset(asset.id.get, "newName")
    updatedAsset.fileName shouldBe "newName"
    updatedAsset.path.get should endWith("newName")

    // get the asset again to make sure it has been updated
    updatedAsset = altitude.service.library.getById(asset.id.get)
    updatedAsset.fileName shouldBe "newName"
    updatedAsset.path.get should endWith("newName")

    // attempt to rename a recycled asset
    asset = altitude.service.library.recycleAsset(asset.id.get)

    intercept[IllegalOperationException] {
      altitude.service.library.renameAsset(asset.id.get, "newName2")
    }
  }

  test("Move recycled asset to folder") {
    val asset: Asset = altitude.service.library.add(makeAsset(altitude.service.folder.triageFolder))
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

    val all = altitude.service.folder.repositoryFolders()

    (altitude.service.folder.getByIdWithChildAssetCounts(folder1.id.get, all): Folder).numOfAssets shouldBe 1
  }

  test("Move asset to a different folder") {
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

    SET_SECOND_REPO()

    altitude.service.library.query(Query()).isEmpty shouldBe true

    altitude.service.library.query(
      Query(params = Map(C.Api.Folder.QUERY_ARG_NAME -> folder1.id.get))
    ).isEmpty shouldBe true
  }

  test("Move asset to same folder") {
    /*
    folder1
    folder2
    */
    val folder1: Folder = altitude.service.folder.addFolder("folder1")

    val asset: Asset = altitude.service.library.add(makeAsset(folder1))

    altitude.service.library.moveAssetToFolder(asset.id.get, folder1.id.get)

    // same but recycled
    altitude.service.library.recycleAsset(asset.id.get)
    altitude.service.library.moveAssetToFolder(asset.id.get, folder1.id.get)
  }

  test("Recycle asset") {
    altitude.service.library.add(makeAsset(altitude.service.folder.triageFolder))

    SET_SECOND_USER()
    altitude.service.library.add(makeAsset(altitude.service.folder.triageFolder))

    SET_FIRST_USER()
    altitude.service.asset.query(Query()).records.length shouldBe 2

    val asset: Asset = altitude.service.asset.query(Query()).records.head
    altitude.service.library.recycleAsset(asset.id.get)

    altitude.service.asset.query(Query()).records.length shouldBe 1
    altitude.service.asset.queryRecycled(Query()).records.length shouldBe 1

    SET_SECOND_REPO()

    altitude.service.asset.queryRecycled(Query()).records.length shouldBe 0
  }

  test("Get recycled asset") {
    val asset: Asset = altitude.service.library.add(makeAsset(altitude.service.folder.triageFolder))
    altitude.service.library.recycleAsset(asset.id.get)
  }

  test("Restore recycled asset") {
    val asset: Asset = altitude.service.library.add(makeAsset(altitude.service.folder.triageFolder))
    val trashed: Asset = altitude.service.library.recycleAsset(asset.id.get)
    altitude.service.library.restoreRecycledAsset(trashed.id.get)
    altitude.service.asset.query(Query()).isEmpty shouldBe false
  }

  test("Restore recycled asset to non-existing folder") {
    val asset: Asset = altitude.service.library.add(makeAsset(altitude.service.folder.triageFolder))
    altitude.service.library.recycleAsset(asset.id.get)

    intercept[NotFoundException] {
      altitude.service.library.moveAssetToFolder(asset.id.get, "bad")
    }
  }

  test("Recycle folder assets") {
    val folder1: Folder = altitude.service.folder.addFolder("folder1")

    val folder2: Folder = altitude.service.folder.addFolder("folder2")

    val folder2_1: Folder = altitude.service.folder.addFolder(
      name = "folder2_1", parentId = folder2.id)

    var asset1: Asset = altitude.service.library.add(makeAsset(folder1))
    var asset2: Asset = altitude.service.library.add(makeAsset(folder2))
    var asset3: Asset = altitude.service.library.add(makeAsset(folder2_1))

    altitude.service.library.deleteFolderById(folder1.id.get)
    altitude.service.library.deleteFolderById(folder2.id.get)

    asset1 = altitude.service.library.getById(asset1.id.get)
    asset2 = altitude.service.library.getById(asset2.id.get)
    asset3 = altitude.service.library.getById(asset3.id.get)

    asset1.isRecycled shouldBe true
    asset2.isRecycled shouldBe true
    asset3.isRecycled shouldBe true
  }

  test("Recycle non-existent folder") {
    val folder1: Folder = altitude.service.folder.addFolder("folder1")
    altitude.service.library.deleteFolderById(folder1.id.get)

    intercept[NotFoundException] {
      altitude.service.library.deleteFolderById(folder1.id.get)
    }
  }

  test("Restore recycled asset twice") {
    val folder1: Folder = altitude.service.folder.addFolder("folder1")

    val asset1: Asset = altitude.service.library.add(makeAsset(folder1))
    val asset2: Asset = altitude.service.library.add(makeAsset(folder1))
    val asset3: Asset = altitude.service.library.add(makeAsset(folder1))
    // assets 1, 2, 3 in folder 1

    altitude.service.library.recycleAssets(Set(asset1.id.get, asset2.id.get, asset3.id.get))
    // assets 1, 2, 3 in trash bin

    altitude.service.library.restoreRecycledAsset(asset1.id.get)
    // asset 1 in folder 1; assets 2, 3 in trash bin

    // throw an exception during file move
    val altitudeSpy = Mockito.spy(altitude)
    val serviceSpy = Mockito.spy(altitude.service)
    val librarySpy = Mockito.spy(altitude.service.library)
    val fileStoreSpy = Mockito.spy(altitude.service.fileStore)
    Mockito.doReturn(serviceSpy, Array.empty:_*).when(altitudeSpy).service
    Mockito.doReturn(fileStoreSpy, Array.empty:_*).when(serviceSpy).fileStore
    Mockito.doReturn(librarySpy, Array.empty:_*).when(serviceSpy).library
    Mockito.doReturn(altitudeSpy, Array.empty:_*).when(librarySpy).app

    savepoint()

    // error
    var numOfCalls = 0 // throw on *second* asset being restored in storage
    Mockito.doAnswer((_: InvocationOnMock) => {
      numOfCalls += 1

      val doThrow = numOfCalls == 2
      if (doThrow) {
        throw StorageException("test")
      }
    }).when(fileStoreSpy).restoreAsset(any())(any(), any())

    intercept[StorageException] {
      altitudeSpy.service.library.restoreRecycledAssets(Set(asset2.id.get, asset3.id.get))
    }
    // asset 1, 2 in folder 1; asset 3 in trash bin

    var stats = altitude.service.stats.getStats

    stats.getStatValue(Stats.TOTAL_ASSETS) shouldBe 3
    stats.getStatValue(Stats.SORTED_ASSETS) shouldBe 1
    stats.getStatValue(Stats.RECYCLED_ASSETS) shouldBe 2

    // success
    Mockito.doCallRealMethod().when(fileStoreSpy).restoreAsset(any())(any(), any())
    altitudeSpy.service.library.restoreRecycledAssets(Set(asset3.id.get))
    // assets 1, 2, 3 in folder 1

    stats = altitude.service.stats.getStats

    stats.getStatValue(Stats.SORTED_ASSETS) shouldBe 2
    stats.getStatValue(Stats.TOTAL_ASSETS) shouldBe 3
    stats.getStatValue(Stats.RECYCLED_ASSETS) shouldBe 1
  }

  test("Restore an asset that was imported again") {
    val folder1: Folder = altitude.service.folder.addFolder("folder1")

    val assetToImport: Asset = makeAsset(folder1)
    val importedAsset: Asset = altitude.service.library.add(assetToImport)

    // recycle the asset
    altitude.service.library.recycleAsset(importedAsset.id.get)

    // import a new copy of it (should be allowed)
    altitude.service.library.add(assetToImport)

    // now restore the previously deleted copy into itself
    intercept[DuplicateException] {
      altitude.service.library.restoreRecycledAsset(importedAsset.id.get)
    }
  }

/*
  test("Restoring an asset into a defunct path should recreate it") {
    val folder1: Folder = altitude.service.folder.addFolder("folder1")

    val assetToImport: Asset = makeAsset(folder1)
    val importedAsset: Asset = altitude.service.library.add(assetToImport)
    altitude.service.library.recycleAsset(importedAsset.id.get)
    altitude.service.library.deleteFolderById(folder1.id.get)
    altitude.service.library.restoreRecycledAsset(importedAsset.id.get)
  }
*/
}

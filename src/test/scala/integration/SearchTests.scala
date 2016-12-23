package integration

import altitude.models.search.Query
import altitude.models.{Asset, AssetType, Folder}
import altitude.{Const => C, Util}
import org.scalatest.DoNotDiscover
import org.scalatest.Matchers._

@DoNotDiscover class SearchTests(val config: Map[String, String]) extends IntegrationTestCore {
  test("empty search") {
    val assets = altitude.service.library.search(new Query(currentUser)).records
    assets.length should be(0)
  }

  test("search root folder") {
    val mediaType = new AssetType(mediaType = "mediaType", mediaSubtype = "mediaSubtype", mime = "mime")
    val asset = new Asset(
      userId = currentUser.id.get,
      repoId = ctx.repo.id.get,
      assetType = mediaType,
      path = "path",
      md5 = "md5",
      folderId = ctx.repo.uncatFolderId,
      sizeBytes = 1L)
    altitude.service.asset.add(asset)

    val assets = altitude.service.library.search(Query(currentUser)).records
    assets.length should be(1)
  }

  test("search uncategorized folder") {
    val mediaType = new AssetType(mediaType = "mediaType", mediaSubtype = "mediaSubtype", mime = "mime")
    val asset = new Asset(
      userId = currentUser.id.get,
      repoId = ctx.repo.id.get,
      assetType = mediaType,
      path = "path",
      md5 = "md5",
      folderId = ctx.repo.uncatFolderId,
      sizeBytes = 1L)
    altitude.service.asset.add(asset)

    val query = Query(currentUser, params = Map(C.Api.Folder.QUERY_ARG_NAME -> ctx.repo.uncatFolderId))
    val assets = altitude.service.library.search(query).records
    assets.length should be(1)
  }

  test("search a folder") {
    /*
  folder1
    folder1_1
    folder1_2
  */
    val folder1: Folder = altitude.service.folder.addFolder("folder1")

    val folder1_1: Folder = altitude.service.folder.addFolder(
      name = "folder1_1", parentId = folder1.id)

    folder1_1.parentId should not be None

    val folder1_2: Folder = altitude.service.folder.addFolder(
      name = "folder1_2", parentId = folder1.id)

    val mediaType = new AssetType(mediaType = "mediaType", mediaSubtype = "mediaSubtype", mime = "mime")

    altitude.service.asset.add(new Asset(
      folderId = folder1_1.id.get.toString,
      repoId = ctx.repo.id.get,
      userId = currentUser.id.get,
      assetType = mediaType,
      path = Util.randomStr(30),
      md5 = Util.randomStr(32),
      sizeBytes = 1L))

    altitude.service.asset.add(new Asset(
      folderId = folder1_2.id.get.toString,
      userId = currentUser.id.get,
      repoId = ctx.repo.id.get,
      assetType = mediaType,
      path = Util.randomStr(30),
      md5 = Util.randomStr(32),
      sizeBytes = 1L))

    altitude.service.asset.add(new Asset(
      folderId = folder1.id.get.toString,
      userId = currentUser.id.get,
      repoId = ctx.repo.id.get,
      assetType = mediaType,
      path = Util.randomStr(30),
      md5 = Util.randomStr(32),
      sizeBytes = 1L))

    altitude.service.library.search(
      Query(currentUser, params = Map(C.Api.Folder.QUERY_ARG_NAME -> folder1_2.id.get))
    ).records.length should be(1)

    altitude.service.library.search(
      Query(currentUser, params = Map(C.Api.Folder.QUERY_ARG_NAME -> folder1_1.id.get))
    ).records.length should be(1)

    altitude.service.library.search(
      Query(currentUser, params = Map(C.Api.Folder.QUERY_ARG_NAME -> folder1.id.get))
    ).records.length should be(3)
  }

  test("folder filtering") {
    /*
    folder1
    folder2
      folder2_1
    */
    val folder1: Folder = altitude.service.folder.addFolder("folder1")

    val folder2: Folder = altitude.service.folder.addFolder("folder2")

    val folder2_1: Folder = altitude.service.folder.addFolder(
      name = "folder2_1", parentId = folder2.id)

    // fill up the hierarchy with assets x times over
    1 to 2 foreach {n =>
      altitude.service.library.add(makeAsset(folder1))
      altitude.service.library.add(makeAsset(folder2))
      altitude.service.library.add(makeAsset(folder2_1))
    }

    altitude.service.library.search(
      Query(currentUser, params = Map(C.Api.Folder.QUERY_ARG_NAME -> folder1.id.get))
    ).records.length shouldEqual 2

    altitude.service.library.search(
      Query(currentUser, params = Map(C.Api.Folder.QUERY_ARG_NAME -> folder2_1.id.get))
    ).records.length shouldEqual 2

    altitude.service.library.search(
      Query(currentUser, params = Map(C.Api.Folder.QUERY_ARG_NAME -> folder2.id.get))
    ).records.length shouldEqual 4
  }

  test("pagination") {
    1 to 6 foreach { n =>
      altitude.service.library.add(makeAsset(altitude.service.folder.getUncatFolder()))
    }

    val q = Query(currentUser, rpp = 2, page = 1)
    val results = altitude.service.library.search(q)
    results.total shouldEqual 6
    results.records.length shouldEqual 2
    results.nonEmpty shouldEqual true
    results.totalPages shouldEqual 3

    val q2 = Query(currentUser, rpp = 2, page = 2)
    val results2 = altitude.service.library.search(q2)
    results2.total shouldEqual 6
    results2.records.length shouldEqual 2
    results2.totalPages shouldEqual 3

    val q3 = Query(currentUser, rpp = 2, page = 3)
    val results3 = altitude.service.library.search(q3)
    results3.total shouldEqual 6
    results3.records.length shouldEqual 2
    results3.totalPages shouldEqual 3

    val q4 = Query(currentUser, rpp = 2, page = 4)
    val results4 = altitude.service.library.search(q4)
    results4.total shouldEqual 6
    results4.records.length shouldEqual 0
    results4.totalPages shouldEqual 3

    val q5 = Query(currentUser, rpp = 6, page = 1)
    val results5 = altitude.service.library.search(q5)
    results5.total shouldEqual 6
    results5.records.length shouldEqual 6
    results5.totalPages shouldEqual 1

    val q6 = Query(currentUser, rpp = 20, page = 1)
    val results6 = altitude.service.library.search(q6)
    results6.total shouldEqual 6
    results6.records.length shouldEqual 6
    results6.totalPages shouldEqual 1
  }
}

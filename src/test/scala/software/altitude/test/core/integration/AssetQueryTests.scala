package software.altitude.test.core.integration

import org.scalatest.DoNotDiscover
import org.scalatest.Matchers._
import software.altitude.core.models.{Asset, AssetType, Folder}
import software.altitude.core.util.Query
import software.altitude.core.{Const => C, Util}

@DoNotDiscover class AssetQueryTests(val config: Map[String, Any]) extends IntegrationTestCore {
  test("Empty search") {
    val results = altitude.service.library.query(new Query())
    results.records.length shouldBe 0
    results.totalPages shouldBe 0
  }

  test("Search root folder") {
    val mediaType = new AssetType(mediaType = "mediaType", mediaSubtype = "mediaSubtype", mime = "mime")
    val asset = new Asset(
      userId = currentUser.id.get,
      assetType = mediaType,
      fileName = "filename.ext",
      path = Some("path"),
      md5 = "md5",
      folderId = ctx.repo.triageFolderId,
      sizeBytes = 1L)
    altitude.service.asset.add(asset)

    val assets = altitude.service.library.query(Query()).records
    assets.length shouldBe 1
  }

  test("Search triage folder") {
    val mediaType = new AssetType(mediaType = "mediaType", mediaSubtype = "mediaSubtype", mime = "mime")
    val asset = new Asset(
      userId = currentUser.id.get,
      assetType = mediaType,
      fileName = "filename.ext",
      path = Some("path"),
      md5 = "md5",
      folderId = ctx.repo.triageFolderId,
      sizeBytes = 1L)
    altitude.service.asset.add(asset)

    val query = Query(params = Map(C.Api.Folder.QUERY_ARG_NAME -> ctx.repo.triageFolderId))
    val assets = altitude.service.library.query(query).records
    assets.length shouldBe 1
  }

  test("Search a folder") {
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
      userId = currentUser.id.get,
      assetType = mediaType,
      fileName = "filename.ext",
      path = Some(Util.randomStr(30)),
      md5 = Util.randomStr(32),
      sizeBytes = 1L))

    altitude.service.asset.add(new Asset(
      folderId = folder1_2.id.get.toString,
      userId = currentUser.id.get,
      assetType = mediaType,
      fileName = "filename.ext",
      path = Some(Util.randomStr(30)),
      md5 = Util.randomStr(32),
      sizeBytes = 1L))

    altitude.service.asset.add(new Asset(
      folderId = folder1.id.get.toString,
      userId = currentUser.id.get,
      assetType = mediaType,
      fileName = "filename.ext",
      path = Some(Util.randomStr(30)),
      md5 = Util.randomStr(32),
      sizeBytes = 1L))

    altitude.service.library.query(
      Query(params = Map(C.Api.Folder.QUERY_ARG_NAME -> folder1_2.id.get))
    ).records.length shouldBe 1

    altitude.service.library.query(
      Query(params = Map(C.Api.Folder.QUERY_ARG_NAME -> folder1_1.id.get))
    ).records.length shouldBe 1

    altitude.service.library.query(
      Query(params = Map(C.Api.Folder.QUERY_ARG_NAME -> folder1.id.get))
    ).records.length shouldBe 3
  }

  test("Folder filtering") {
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

    altitude.service.library.query(
      Query(params = Map(C.Api.Folder.QUERY_ARG_NAME -> folder1.id.get))
    ).records.length shouldBe 2

    altitude.service.library.query(
      Query(params = Map(C.Api.Folder.QUERY_ARG_NAME -> folder2_1.id.get))
    ).records.length shouldBe 2

    altitude.service.library.query(
      Query(params = Map(C.Api.Folder.QUERY_ARG_NAME -> folder2.id.get))
    ).records.length shouldBe 4
  }

  test("Pagination") {
    1 to 6 foreach { n =>
      altitude.service.library.add(makeAsset(altitude.service.folder.triageFolder))
    }

    val q = Query(rpp = 2, page = 1)
    val results = altitude.service.library.query(q)
    results.total shouldBe 6
    results.records.length shouldBe 2
    results.nonEmpty shouldBe true
    results.totalPages shouldBe 3

    val q2 = Query(rpp = 2, page = 2)
    val results2 = altitude.service.library.query(q2)
    results2.total shouldBe 6
    results2.records.length shouldBe 2
    results2.totalPages shouldBe 3

    val q3 = Query(rpp = 2, page = 3)
    val results3 = altitude.service.library.query(q3)
    results3.total shouldBe 6
    results3.records.length shouldBe 2
    results3.totalPages shouldBe 3

    val q4 = Query(rpp = 2, page = 4)
    val results4 = altitude.service.library.query(q4)
    results4.total shouldBe 6
    results4.records.length shouldBe 0
    results4.totalPages shouldBe 0

    val q5 = Query(rpp = 6, page = 1)
    val results5 = altitude.service.library.query(q5)
    results5.total shouldBe 6
    results5.records.length shouldBe 6
    results5.totalPages shouldBe 1

    val q6 = Query(rpp = 20, page = 1)
    val results6 = altitude.service.library.query(q6)
    results6.total shouldBe 6
    results6.records.length shouldBe 6
    results6.totalPages shouldBe 1
  }

  test("Triage and Trash should have correct totals in query results") {
    val asset: Asset = altitude.service.library.add(makeAsset(altitude.service.folder.triageFolder))
    altitude.service.library.add(makeAsset(altitude.service.folder.triageFolder))

    altitude.service.library.recycleAsset(asset.id.get)

    /* We now have two assets. One in triage, one trash. Each of the totals should be "1" not "2"
     */

    val query = Query(params = Map(C.Api.Folder.QUERY_ARG_NAME -> ctx.repo.triageFolderId))
    var results = altitude.service.library.query(query)
    results.records.length shouldBe 1
    results.total shouldBe 1

    results = altitude.service.library.queryRecycled(Query())
    results.records.length shouldBe 1
    results.total shouldBe 1
  }

}
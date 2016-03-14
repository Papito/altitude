package integration

import altitude.models.search.Query
import altitude.models.{Asset, Folder, MediaType}
import altitude.{Const => C, Util}
import org.scalatest.DoNotDiscover
import org.scalatest.Matchers._

@DoNotDiscover class SearchTests(val config: Map[String, String]) extends IntegrationTestCore {
  test("empty search test") {
    val assets: List[Asset] = altitude.service.library.search(new Query())
    assets.length should be(0)
  }

  test("test search root folder") {
    val mediaType = new MediaType(mediaType = "mediaType", mediaSubtype = "mediaSubtype", mime = "mime")
    val asset = new Asset(
      mediaType = mediaType,
      path = "path",
      md5 = "md5",
      folderId = Folder.UNCATEGORIZED.id.get,
      sizeBytes = 1L)
    altitude.service.asset.add(asset)

    val assets: List[Asset] = altitude.service.library.search(Query())
    assets.length should be(1)
  }

  test("test search uncategorized folder") {
    val mediaType = new MediaType(mediaType = "mediaType", mediaSubtype = "mediaSubtype", mime = "mime")
    val asset = new Asset(
      mediaType = mediaType,
      path = "path",
      md5 = "md5",
      folderId = Folder.UNCATEGORIZED.id.get,
      sizeBytes = 1L)
    altitude.service.asset.add(asset)

    val query = Query(params = Map(C("Api.Folder.QUERY_ARG_NAME") -> Folder.UNCATEGORIZED.id.get))
    val assets: List[Asset] = altitude.service.library.search(query)
    assets.length should be(1)
  }

  test("test search a folder") {
    /*
  folder1
    folder1_1
    folder1_2
  */
    val folder1: Folder = altitude.service.folder.add(
      Folder(name = "folder1"))
    
    val folder1_1: Folder = altitude.service.folder.add(
      Folder(name = "folder1_1", parentId = folder1.id.get))

    folder1_1.parentId should not be None

    val folder1_2: Folder = altitude.service.folder.add(
      Folder(name = "folder1_2", parentId = folder1.id.get))

    val mediaType = new MediaType(mediaType = "mediaType", mediaSubtype = "mediaSubtype", mime = "mime")

    altitude.service.asset.add(new Asset(folderId = folder1_1.id.get.toString,
      mediaType = mediaType, path = Util.randomStr(30), md5 = Util.randomStr(30), sizeBytes = 1L))

    altitude.service.asset.add(new Asset(folderId = folder1_2.id.get.toString,
      mediaType = mediaType, path = Util.randomStr(30), md5 = Util.randomStr(30), sizeBytes = 1L))

    altitude.service.asset.add(new Asset(folderId = folder1.id.get.toString,
      mediaType = mediaType, path = Util.randomStr(30), md5 = Util.randomStr(30), sizeBytes = 1L))

    altitude.service.library.search(
      Query(params = Map(C("Api.Folder.QUERY_ARG_NAME") -> folder1_2.id.get))
    ).length should be(1)

    altitude.service.library.search(
      Query(params = Map(C("Api.Folder.QUERY_ARG_NAME") -> folder1_1.id.get))
    ).length should be(1)

    altitude.service.library.search(
      Query(params = Map(C("Api.Folder.QUERY_ARG_NAME") -> folder1.id.get))
    ).length should be(3)
  }
}

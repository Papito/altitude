package integration

import altitude.{Const => C, Util}
import altitude.exceptions.NotFoundException
import altitude.models.search.Query
import altitude.models.{Asset, MediaType, Folder}
import org.scalatest.DoNotDiscover
import org.scalatest.Matchers._

@DoNotDiscover class AssetServiceTests (val config: Map[String, String]) extends IntegrationTestCore {
  test("get asset by invalid id") {
    intercept[NotFoundException] {
      altitude.service.library.getById("invalid")
    }
  }

  test("get preview by invalid asset id") {
    intercept[NotFoundException] {
      altitude.service.library.getPreview("invalid")
    }
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

    val mediaType = new MediaType(mediaType = "mediaType", mediaSubtype = "mediaSubtype", mime = "mime")

    val asset: Asset = altitude.service.asset.add(new Asset(folderId = folder1.id.get.toString,
      mediaType = mediaType, path = Util.randomStr(30), md5 = Util.randomStr(30), sizeBytes = 1L))

    altitude.service.library.search(
      Query(params = Map(C.Api.Folder.QUERY_ARG_NAME -> folder1.id.get))
    ).length should be(1)

    altitude.service.library.moveToFolder(asset.id.get, folder2.id.get)

    altitude.service.library.search(
      Query(params = Map(C.Api.Folder.QUERY_ARG_NAME -> folder1.id.get))
    ).length should be(0)

    altitude.service.library.search(
      Query(params = Map(C.Api.Folder.QUERY_ARG_NAME -> folder2.id.get))
    ).length should be(1)

  }
}

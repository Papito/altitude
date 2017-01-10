package integration

import altitude.models.{Asset, Folder}
import org.scalatest.DoNotDiscover

@DoNotDiscover class SearchServiceTests(val config: Map[String, String]) extends IntegrationTestCore {

  test("index and search assets") {
    val folder1: Folder = altitude.service.folder.addFolder("folder1")
    val asset: Asset = altitude.service.library.add(makeAsset(folder1))
    altitude.service.search.indexAsset(asset)
  }
}

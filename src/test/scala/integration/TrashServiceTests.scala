package integration

import altitude.models.{Asset, Folder}
import org.scalatest.Matchers._
import org.scalatest.DoNotDiscover

@DoNotDiscover class TrashServiceTests(val config: Map[String, String]) extends IntegrationTestCore {

  test("move to trash") {
    val asset: Asset = altitude.service.library.add(makeAsset(Folder.UNCATEGORIZED))
    altitude.service.asset.getAll.length should be (1)
    altitude.service.library.recycleAsset(asset.id.get)
    altitude.service.asset.getAll.length should be (0)
    altitude.service.trash.getAll.length should be (1)
  }
}
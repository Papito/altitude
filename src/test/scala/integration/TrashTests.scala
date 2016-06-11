package integration

import altitude.models.{Asset, Folder}
import org.scalatest.Matchers._
import org.scalatest.DoNotDiscover

@DoNotDiscover class TrashTests (val config: Map[String, String]) extends IntegrationTestCore {

  test("move to trash") {
    val asset: Asset = altitude.service.library.add(makeAsset(Folder.UNCATEGORIZED))
    altitude.service.asset.getAll.length should be (1)
    altitude.service.trash.recycle(asset.id.get)
    altitude.service.asset.getAll.length should be (0)
    altitude.service.trash.getAll.length should be (1)
  }
}

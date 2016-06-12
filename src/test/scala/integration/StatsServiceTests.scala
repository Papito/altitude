package integration

import altitude.models.{Stats, Asset, Folder}
import org.scalatest.Matchers._
import org.scalatest.DoNotDiscover

@DoNotDiscover class StatsServiceTests(val config: Map[String, String]) extends IntegrationTestCore {

  test("test all stats") {
    // create an asset in a folder
    val folder1: Folder = altitude.service.folder.add(
      Folder(name = "folder1"))

    altitude.service.library.add(makeAsset(folder1))

    // create an uncategorized asset
    altitude.service.library.add(makeAsset(Folder.UNCATEGORIZED))

    // create an asset and delete it
    val assetToDelete: Asset = altitude.service.library.add(makeAsset(Folder.UNCATEGORIZED))
    altitude.service.trash.deleteById(assetToDelete.id.get)

    val stats = altitude.service.stats.getStats

    stats.getStatValue(Stats.TOTAL_ASSETS) should be (2)
  }
}

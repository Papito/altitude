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
    val uncategorizedAsset: Asset = altitude.service.library.add(makeAsset(Folder.UNCATEGORIZED))

    // create an asset and delete it
    val assetToDelete1: Asset = altitude.service.library.add(makeAsset(folder1))
    altitude.service.library.recycleAsset(assetToDelete1.id.get)
    val assetToDelete2: Asset = altitude.service.library.add(makeAsset(Folder.UNCATEGORIZED))
    altitude.service.library.recycleAsset(assetToDelete2.id.get)

    val stats = altitude.service.stats.getStats
    stats.getStatValue(Stats.TOTAL_ASSETS) should be (2)
    stats.getStatValue(Stats.RECYCLED_ASSETS) should be (2)
    stats.getStatValue(Stats.UNCATEGORIZED_ASSETS) should be (1)

    altitude.service.library.moveAssetToFolder(uncategorizedAsset.id.get, folder1.id.get)

    val stats2 = altitude.service.stats.getStats
    stats2.getStatValue(Stats.TOTAL_ASSETS) should be (2)
    stats2.getStatValue(Stats.RECYCLED_ASSETS) should be (2)
    stats2.getStatValue(Stats.UNCATEGORIZED_ASSETS) should be (0)
  }

  test("test uncategorized") {
    // create an asset in a folder
    val folder1: Folder = altitude.service.folder.add(
      Folder(name = "folder1"))

    val asset: Asset = altitude.service.library.add(makeAsset(folder1))

    altitude.service.library.moveAssetToUncategorized(asset.id.get)

    val stats = altitude.service.stats.getStats
    stats.getStatValue(Stats.TOTAL_ASSETS) should be (1)
    stats.getStatValue(Stats.UNCATEGORIZED_ASSETS) should be (1)
  }
}

package integration

import altitude.models.{Asset, Folder, Stats, Trash}
import org.scalatest.DoNotDiscover
import org.scalatest.Matchers._

@DoNotDiscover class StatsServiceTests(val config: Map[String, String]) extends IntegrationTestCore {

  test("test totals") {
    // create an asset in a folder
    val folder1: Folder = altitude.service.folder.addFolder("folder1")

    altitude.service.library.add(makeAsset(folder1))

    // create an uncategorized asset
    val uncategorizedAsset: Asset = altitude.service.library.add(makeAsset(
      altitude.service.folder.getUncatFolder))

    // create an asset and delete it
    val assetToDelete1: Asset = altitude.service.library.add(makeAsset(folder1))
    altitude.service.library.recycleAsset(assetToDelete1.id.get)
    val assetToDelete2: Asset = altitude.service.library.add(makeAsset(
      altitude.service.folder.getUncatFolder))
    altitude.service.library.recycleAsset(assetToDelete2.id.get)

    val stats = altitude.service.stats.getStats
    println(stats)
    stats.getStatValue(Stats.TOTAL_ASSETS) should be (2)
    stats.getStatValue(Stats.RECYCLED_ASSETS) should be (2)
    stats.getStatValue(Stats.UNCATEGORIZED_ASSETS) should be (1)

    altitude.service.library.moveAssetToFolder(uncategorizedAsset.id.get, folder1.id.get)

    val stats2 = altitude.service.stats.getStats
    stats2.getStatValue(Stats.TOTAL_ASSETS) should be (2)
    stats2.getStatValue(Stats.RECYCLED_ASSETS) should be (2)
    stats2.getStatValue(Stats.UNCATEGORIZED_ASSETS) should be (0)

    SET_SECONDARY_REPO()
    val stats3 = altitude.service.stats.getStats

    // this will be passing later
    intercept[RuntimeException] {
      stats3.getStatValue(Stats.TOTAL_ASSETS)
    }
  }

  test("test uncategorized") {
    // create an asset in a folder
    val folder1: Folder = altitude.service.folder.addFolder("folder1")

    val asset: Asset = altitude.service.library.add(makeAsset(folder1))

    altitude.service.library.moveAssetToUncategorized(asset.id.get)

    val stats = altitude.service.stats.getStats
    stats.getStatValue(Stats.TOTAL_ASSETS) should be (1)
    stats.getStatValue(Stats.UNCATEGORIZED_ASSETS) should be (1)
  }

  test("test move recycled asset to folder") {
    val asset: Asset = altitude.service.library.add(makeAsset(
      altitude.service.folder.getUncatFolder))

    altitude.service.library.recycleAsset(asset.id.get)

    val folder1: Folder = altitude.service.folder.addFolder("folder1")

    altitude.service.library.moveRecycledAssetToFolder(asset.id.get, folder1.id.get)

    val stats = altitude.service.stats.getStats
    stats.getStatValue(Stats.TOTAL_ASSETS) should be (1)
    stats.getStatValue(Stats.RECYCLED_ASSETS) should be (0)
  }

  test("restore recycled asset") {
    val asset: Asset = altitude.service.library.add(makeAsset(
      altitude.service.folder.getUncatFolder))
    val trashed: Trash = altitude.service.library.recycleAsset(asset.id.get)
    altitude.service.library.restoreRecycledAsset(trashed.id.get)

    SET_SECONDARY_USER()
    altitude.service.library.add(makeAsset(
      altitude.service.folder.getUncatFolder))

    SET_PRIMARY_USER()

    val stats = altitude.service.stats.getStats
    stats.getStatValue(Stats.TOTAL_ASSETS) should be (2)
    stats.getStatValue(Stats.RECYCLED_ASSETS) should be (0)

    SET_SECONDARY_REPO()
    val stats2 = altitude.service.stats.getStats

    // this will be passing later
    intercept[RuntimeException] {
      stats2.getStatValue(Stats.TOTAL_ASSETS)
    }

  }
}

package integration

import altitude.models.{Asset, Folder, Stats}
import org.scalatest.DoNotDiscover
import org.scalatest.Matchers._

@DoNotDiscover class StatsServiceTests(val config: Map[String, Any]) extends IntegrationTestCore {

  test("test totals") {
    // create an asset in a folder
    val folder1: Folder = altitude.service.folder.addFolder("folder1")

    altitude.service.library.add(makeAsset(folder1))

    // create an unsorted asset
    val unsortededAsset: Asset = altitude.service.library.add(makeAsset(
      altitude.service.folder.getUnsortedFolder))

    // create an asset and delete it
    val assetToDelete1: Asset = altitude.service.library.add(makeAsset(folder1))
    altitude.service.library.recycleAsset(assetToDelete1.id.get)
    val assetToDelete2: Asset = altitude.service.library.add(makeAsset(
      altitude.service.folder.getUnsortedFolder))
    altitude.service.library.recycleAsset(assetToDelete2.id.get)

    val stats = altitude.service.stats.getStats
    stats.getStatValue(Stats.TOTAL_ASSETS) should be (2)
    stats.getStatValue(Stats.RECYCLED_ASSETS) should be (2)
    stats.getStatValue(Stats.UNSORTED_ASSETS) should be (1)

    altitude.service.library.moveAssetToFolder(unsortededAsset.id.get, folder1.id.get)

    val stats2 = altitude.service.stats.getStats
    stats2.getStatValue(Stats.TOTAL_ASSETS) should be (2)
    stats2.getStatValue(Stats.RECYCLED_ASSETS) should be (2)
    stats2.getStatValue(Stats.UNSORTED_ASSETS) should be (0)

    SET_SECONDARY_REPO()
    val stats3 = altitude.service.stats.getStats

    // this will be passing later
    intercept[RuntimeException] {
      stats3.getStatValue(Stats.TOTAL_ASSETS)
    }
  }

  test("test unsorted") {
    // create an asset in a folder
    val folder1: Folder = altitude.service.folder.addFolder("folder1")

    val asset: Asset = altitude.service.library.add(makeAsset(folder1))

    altitude.service.library.moveAssetToUnsorted(asset.id.get)

    val stats = altitude.service.stats.getStats
    stats.getStatValue(Stats.TOTAL_ASSETS) should be (1)
    stats.getStatValue(Stats.UNSORTED_ASSETS) should be (1)
  }

  test("test move recycled asset to folder") {
    val asset: Asset = altitude.service.library.add(makeAsset(
      altitude.service.folder.getUnsortedFolder))

    altitude.service.library.recycleAsset(asset.id.get)

    val folder1: Folder = altitude.service.folder.addFolder("folder1")

    altitude.service.library.moveRecycledAssetToFolder(asset.id.get, folder1.id.get)

    val stats = altitude.service.stats.getStats
    stats.getStatValue(Stats.TOTAL_ASSETS) should be (1)
    stats.getStatValue(Stats.RECYCLED_ASSETS) should be (0)
  }

  test("restore recycled asset") {
    val asset: Asset = altitude.service.library.add(makeAsset(
      altitude.service.folder.getUnsortedFolder))
    val trashed: Asset = altitude.service.library.recycleAsset(asset.id.get)
    altitude.service.library.restoreRecycledAsset(trashed.id.get)

    SET_SECONDARY_USER()
    altitude.service.library.add(makeAsset(
      altitude.service.folder.getUnsortedFolder))

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

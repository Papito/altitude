package software.altitude.test.core.integration

import org.scalatest.DoNotDiscover
import org.scalatest.Matchers._
import software.altitude.core.models.{Asset, Folder, Stats}
import software.altitude.core.util.Query

@DoNotDiscover class StatsServiceTests(val config: Map[String, Any]) extends IntegrationTestCore {

  val ASSET_SIZE = 1000

  test("Test totals (simple cases)") {
    // create an asset in a folder
    val folder1: Folder = altitude.service.folder.addFolder("folder1")

    altitude.service.library.add(makeAsset(folder1))

    // create a triaged asset
    val triagedAsset: Asset = altitude.service.library.add(makeAsset(
      altitude.service.folder.triageFolder))

    // create an asset and delete it
    val assetToDelete1: Asset = altitude.service.library.add(makeAsset(folder1))
    altitude.service.library.recycleAsset(assetToDelete1.id.get)
    val assetToDelete2: Asset = altitude.service.library.add(makeAsset(
      altitude.service.folder.triageFolder))
    altitude.service.library.recycleAsset(assetToDelete2.id.get)

    val stats = altitude.service.stats.getStats
    stats.getStatValue(Stats.SORTED_ASSETS) shouldBe 1
    stats.getStatValue(Stats.SORTED_BYTES) shouldBe
      stats.getStatValue(Stats.SORTED_ASSETS) * ASSET_SIZE
    stats.getStatValue(Stats.RECYCLED_ASSETS) shouldBe 2
    stats.getStatValue(Stats.RECYCLED_BYTES) shouldBe
      stats.getStatValue(Stats.RECYCLED_ASSETS) * ASSET_SIZE
    stats.getStatValue(Stats.TRIAGE_ASSETS) shouldBe 1
    stats.getStatValue(Stats.TOTAL_ASSETS) shouldBe 4
    stats.getStatValue(Stats.TOTAL_BYTES) shouldBe
      stats.getStatValue(Stats.TOTAL_ASSETS) * ASSET_SIZE

    altitude.service.library.moveAssetToFolder(triagedAsset.id.get, folder1.id.get)

    val stats2 = altitude.service.stats.getStats
    stats2.getStatValue(Stats.SORTED_ASSETS) shouldBe 2
    stats.getStatValue(Stats.SORTED_BYTES) shouldBe
      stats.getStatValue(Stats.SORTED_ASSETS) * ASSET_SIZE
    stats2.getStatValue(Stats.TRIAGE_ASSETS) shouldBe 0
    stats.getStatValue(Stats.TRIAGE_BYTES) shouldBe
      stats.getStatValue(Stats.TRIAGE_ASSETS) * ASSET_SIZE
    stats2.getStatValue(Stats.TOTAL_ASSETS) shouldBe 4
    stats.getStatValue(Stats.TOTAL_BYTES) shouldBe
      stats.getStatValue(Stats.TOTAL_ASSETS) * ASSET_SIZE

    SET_SECOND_REPO()
    val stats3 = altitude.service.stats.getStats

    stats3.getStatValue(Stats.SORTED_ASSETS) shouldBe 0
    stats3.getStatValue(Stats.SORTED_BYTES) shouldBe
      stats3.getStatValue(Stats.SORTED_ASSETS) * ASSET_SIZE
    stats2.getStatValue(Stats.TRIAGE_ASSETS) shouldBe 0
    stats3.getStatValue(Stats.TRIAGE_BYTES) shouldBe
      stats3.getStatValue(Stats.TRIAGE_ASSETS) * ASSET_SIZE
    stats3.getStatValue(Stats.TOTAL_ASSETS) shouldBe 0
    stats3.getStatValue(Stats.TOTAL_BYTES) shouldBe
      stats3.getStatValue(Stats.TOTAL_ASSETS) * ASSET_SIZE
  }

  test("Test triage") {
    // create an asset in a folder
    val folder1: Folder = altitude.service.folder.addFolder("folder1")

    val asset: Asset = altitude.service.library.add(makeAsset(folder1))

    altitude.service.library.moveAssetToTriage(asset.id.get)

    val stats = altitude.service.stats.getStats
    stats.getStatValue(Stats.SORTED_ASSETS) shouldBe 0
    stats.getStatValue(Stats.SORTED_BYTES) shouldBe 0
    stats.getStatValue(Stats.TRIAGE_ASSETS) shouldBe 1
    stats.getStatValue(Stats.TRIAGE_BYTES) shouldBe
      stats.getStatValue(Stats.TRIAGE_ASSETS) * ASSET_SIZE
  }

  test("Test move recycled asset to new folder") {
    var folder1: Folder = altitude.service.folder.addFolder("folder1")

    val asset: Asset = altitude.service.library.add(makeAsset(folder1))

    folder1 = altitude.service.folder.getById(folder1.id.get)
    folder1.numOfAssets shouldBe 1

    altitude.service.library.recycleAsset(asset.id.get)

    var folder2: Folder = altitude.service.folder.addFolder("folder2")

    altitude.service.library.moveAssetToFolder(asset.id.get, folder2.id.get)

    folder1 = altitude.service.folder.getById(folder1.id.get)
    folder1.numOfAssets shouldBe 0

    folder2 = altitude.service.folder.getById(folder2.id.get)
    folder2.numOfAssets shouldBe 1

    val stats = altitude.service.stats.getStats
    stats.getStatValue(Stats.SORTED_ASSETS) shouldBe 1
    stats.getStatValue(Stats.SORTED_BYTES) shouldBe
      stats.getStatValue(Stats.SORTED_ASSETS) * ASSET_SIZE
    stats.getStatValue(Stats.RECYCLED_ASSETS) shouldBe 0
    stats.getStatValue(Stats.RECYCLED_BYTES) shouldBe 0
  }

  test("Test move recycled asset to original folder") {
    var folder1: Folder = altitude.service.folder.addFolder("folder1")

    val asset: Asset = altitude.service.library.add(makeAsset(folder1))

    folder1 = altitude.service.folder.getById(folder1.id.get)
    folder1.numOfAssets shouldBe 1

    altitude.service.library.recycleAsset(asset.id.get)
    folder1 = altitude.service.folder.getById(folder1.id.get)
    folder1.numOfAssets shouldBe 0

    altitude.service.library.moveAssetToFolder(asset.id.get, folder1.id.get)

    folder1 = altitude.service.folder.getById(folder1.id.get)
    folder1.numOfAssets shouldBe 1

    val stats = altitude.service.stats.getStats
    stats.getStatValue(Stats.SORTED_ASSETS) shouldBe 1
    stats.getStatValue(Stats.SORTED_BYTES) shouldBe
      stats.getStatValue(Stats.SORTED_ASSETS) * ASSET_SIZE
    stats.getStatValue(Stats.RECYCLED_ASSETS) shouldBe 0
    stats.getStatValue(Stats.RECYCLED_BYTES) shouldBe 0
  }

  test("Restore recycled asset to triage") {
    val asset: Asset = altitude.service.library.add(makeAsset(altitude.service.folder.triageFolder))

    // second asset for some chaos
    altitude.service.library.add(makeAsset(altitude.service.folder.triageFolder))

    val trashed: Asset = altitude.service.library.recycleAsset(asset.id.get)
    altitude.service.library.restoreRecycledAsset(trashed.id.get)

    SET_SECOND_REPO()

    altitude.service.library.add(makeAsset(altitude.service.folder.triageFolder))

    SET_FIRST_REPO()

    val stats = altitude.service.stats.getStats
    stats.getStatValue(Stats.TOTAL_ASSETS) shouldBe 2
    stats.getStatValue(Stats.TRIAGE_ASSETS) shouldBe 2
    stats.getStatValue(Stats.TRIAGE_BYTES) shouldBe
      stats.getStatValue(Stats.TRIAGE_ASSETS) * ASSET_SIZE
    stats.getStatValue(Stats.SORTED_ASSETS) shouldBe 0
    stats.getStatValue(Stats.SORTED_BYTES) shouldBe 0
    stats.getStatValue(Stats.RECYCLED_ASSETS) shouldBe 0
    stats.getStatValue(Stats.RECYCLED_BYTES) shouldBe 0

    SET_SECOND_REPO()

    val stats2 = altitude.service.stats.getStats

    stats2.getStatValue(Stats.TOTAL_ASSETS) shouldBe 1
    stats2.getStatValue(Stats.TRIAGE_ASSETS) shouldBe 1
    stats2.getStatValue(Stats.TRIAGE_BYTES) shouldBe
      stats2.getStatValue(Stats.TRIAGE_ASSETS) * ASSET_SIZE
    stats2.getStatValue(Stats.SORTED_ASSETS) shouldBe 0
    stats2.getStatValue(Stats.SORTED_BYTES) shouldBe 0
    stats2.getStatValue(Stats.RECYCLED_ASSETS) shouldBe 0
    stats2.getStatValue(Stats.RECYCLED_BYTES) shouldBe 0
  }

  test("Restore recycled asset to original folder") {
    var folder1: Folder = altitude.service.folder.addFolder("folder1")

    val asset: Asset = altitude.service.library.add(makeAsset(folder1))

    val trashed: Asset = altitude.service.library.recycleAsset(asset.id.get)
    folder1 = altitude.service.folder.getById(folder1.id.get)
    folder1.numOfAssets shouldBe 0

    altitude.service.library.restoreRecycledAsset(trashed.id.get)

    folder1 = altitude.service.folder.getById(folder1.id.get)
    folder1.numOfAssets shouldBe 1
  }

  test("Recycle multiple assets") {
    val folder1: Folder = altitude.service.folder.addFolder("folder1")

    1 to 2 foreach { n =>
      altitude.service.library.add(makeAsset(altitude.service.folder.triageFolder))
      altitude.service.library.add(makeAsset(folder1))
    }

    var stats = altitude.service.stats.getStats
    stats.getStatValue(Stats.SORTED_ASSETS) shouldBe 2
    stats.getStatValue(Stats.SORTED_BYTES) shouldBe
      stats.getStatValue(Stats.SORTED_ASSETS) * ASSET_SIZE

    val all: List[Asset] = altitude.service.asset.query(Query()).records.map(Asset.fromJson)

    altitude.service.library.recycleAssets(all.map(_.id.get).toSet)

    stats = altitude.service.stats.getStats
    stats.getStatValue(Stats.SORTED_ASSETS) shouldBe 0
    stats.getStatValue(Stats.SORTED_BYTES) shouldBe 0
    stats.getStatValue(Stats.RECYCLED_ASSETS) shouldBe 4
    stats.getStatValue(Stats.RECYCLED_BYTES) shouldBe
      stats.getStatValue(Stats.RECYCLED_ASSETS) * ASSET_SIZE
  }

  test("Recycle a folder") {
    val folder1: Folder = altitude.service.folder.addFolder("folder1")
    val folder2: Folder = altitude.service.folder.addFolder("folder2")

    1 to 2 foreach { n =>
      altitude.service.library.add(makeAsset(folder1))
      altitude.service.library.add(makeAsset(folder2))
    }

    var stats = altitude.service.stats.getStats
    stats.getStatValue(Stats.SORTED_ASSETS) shouldBe 4

    altitude.service.library.deleteFolderById(folder1.id.get)

    stats = altitude.service.stats.getStats
    stats.getStatValue(Stats.SORTED_ASSETS) shouldBe 2
    stats.getStatValue(Stats.SORTED_BYTES) shouldBe
      stats.getStatValue(Stats.SORTED_ASSETS) * ASSET_SIZE
    stats.getStatValue(Stats.RECYCLED_ASSETS) shouldBe 2
    stats.getStatValue(Stats.RECYCLED_BYTES) shouldBe
      stats.getStatValue(Stats.RECYCLED_ASSETS) * ASSET_SIZE
  }
}

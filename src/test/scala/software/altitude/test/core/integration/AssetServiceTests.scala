package software.altitude.test.core.integration

import org.scalatest.DoNotDiscover
import org.scalatest.matchers.should.Matchers.convertToAnyShouldWrapper
import software.altitude.core.Altitude
import software.altitude.core.FieldConst
import software.altitude.core.NotFoundException
import software.altitude.core.models.Asset
import software.altitude.core.util.Query
import software.altitude.test.core.IntegrationTestCore


@DoNotDiscover class AssetServiceTests (override val testApp: Altitude) extends IntegrationTestCore {
  test("Getting asset by invalid ID should raise NotFoundException") {
    intercept[NotFoundException] {
      testApp.service.library.getById("invalid")
    }
  }

  test("Getting preview by invalid asset ID should raise NotFoundException") {
    intercept[NotFoundException] {
      testApp.service.library.getPreview("invalid")
    }
  }

  test("Should be able to update 'isRecycled' property with 'updateById()'") {
    val asset: Asset = testContext.persistAsset()
    asset.isRecycled shouldBe false

    testApp.service.asset.updateById(
      asset.persistedId,
      Map(FieldConst.Asset.IS_RECYCLED -> true))

    (testApp.service.library.getById(asset.persistedId): Asset).isRecycled shouldBe true
  }

  test("Should be able to query by the recycled property") {
    testContext.persistAsset()

    val q = new Query(params = Map(FieldConst.Asset.IS_RECYCLED -> false))
    val result = testApp.service.asset.query(q)

    result.total shouldBe 1
  }
}

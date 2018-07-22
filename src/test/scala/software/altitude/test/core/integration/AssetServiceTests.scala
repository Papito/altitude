package software.altitude.test.core.integration

import org.scalatest.DoNotDiscover
import play.api.libs.json.Json
import software.altitude.core.NotFoundException
import software.altitude.core.models.Asset
import software.altitude.core.{Const => C, _}
import org.scalatest.Matchers._


@DoNotDiscover class AssetServiceTests (val config: Map[String, Any]) extends IntegrationTestCore {
  test("Getting asset by invalid ID should raise NotFoundException") {
    intercept[NotFoundException] {
      altitude.service.library.getById("invalid")
    }
  }

  test("Getting preview by invalid asset ID should raise NotFoundException") {
    intercept[NotFoundException] {
      altitude.service.library.getPreview("invalid")
    }
  }

  test("Should be able to update 'isRecycled' property with 'updateById()'") {
    val triageFolder = altitude.service.folder.triageFolder
    val asset: Asset = altitude.service.library.add(makeAsset(triageFolder))
    asset.isRecycled shouldBe false

    val updateAsset: Asset = asset.modify(C.Asset.IS_RECYCLED -> true)

    altitude.service.asset.updateById(
      asset.id.get, updateAsset,
      fields = List(C.Asset.IS_RECYCLED))

    (altitude.service.library.getById(asset.id.get): Asset).isRecycled shouldBe true
  }
}

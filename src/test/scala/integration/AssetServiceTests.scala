package integration

import altitude.exceptions.NotFoundException
import org.scalatest.DoNotDiscover

@DoNotDiscover class AssetServiceTests (val config: Map[String, String]) extends IntegrationTestCore {
  test("get asset by invalid id") {
    intercept[NotFoundException] {
      altitude.service.library.getById("invalid")
    }
  }
  test("get preview by invalid asset id") {
    intercept[NotFoundException] {
      altitude.service.library.getPreview("invalid")
    }
  }
}

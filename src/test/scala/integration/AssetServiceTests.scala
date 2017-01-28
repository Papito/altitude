package integration

import altitude.exceptions.NotFoundException
import altitude.{Const => C}
import org.scalatest.DoNotDiscover

@DoNotDiscover class AssetServiceTests (val config: Map[String, Any]) extends IntegrationTestCore {
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

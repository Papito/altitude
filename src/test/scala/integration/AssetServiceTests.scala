package integration

import altitude.exceptions.NotFoundException

class AssetServiceTests (val config: Map[String, String]) extends IntegrationTestCore {
  test("get by invalid id") {
    intercept[NotFoundException] {
      altitude.service.library.getById("invalid")
    }
  }
}

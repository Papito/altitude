package integration

import altitude.{Const => C, Util}
import altitude.exceptions.NotFoundException
import altitude.models.search.Query
import altitude.models.{Asset, MediaType, Folder}
import org.scalatest.DoNotDiscover
import org.scalatest.Matchers._

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

package integration

import altitude.models.Asset
import altitude.models.search.Query
import org.scalatest.DoNotDiscover
import org.scalatest.Matchers._

@DoNotDiscover class SearchTests(val config: Map[String, String]) extends IntegrationTestCore {
  test("import image (JPEG)") {
    val assets: List[Asset] = altitude.service.library.search(new Query())
    assets.length should be(0)
  }

}

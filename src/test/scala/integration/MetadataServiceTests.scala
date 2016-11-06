package integration

import altitude.models.{User, Trash, Asset, Folder}
import org.scalatest.DoNotDiscover
import org.scalatest.Matchers._
import play.api.libs.json.JsObject

@DoNotDiscover class MetadataServiceTests(val config: Map[String, String]) extends IntegrationTestCore {

  test("add user metadata field") {
    altitude.service.library.add(makeAsset(altitude.service.folder.getUserUncatFolder()))
  }
}

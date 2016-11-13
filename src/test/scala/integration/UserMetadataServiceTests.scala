package integration

import altitude.models._
import org.scalatest.DoNotDiscover
import org.scalatest.Matchers._
import play.api.libs.json.JsObject

@DoNotDiscover class UserMetadataServiceTests(val config: Map[String, String]) extends IntegrationTestCore {

  test("add user metadata field") {
    //altitude.service.library.add(makeAsset(altitude.service.folder.getUserUncatFolder()))
    val metadataField = MetadataField(userId = CURRENT_USER_ID, name = "fieldName", fieldType = "type")
    altitude.service.userMetadata.addField(metadataField)
  }
}

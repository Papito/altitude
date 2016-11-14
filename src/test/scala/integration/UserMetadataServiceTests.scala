package integration

import altitude.models._
import org.scalatest.DoNotDiscover
import org.scalatest.Matchers._
import play.api.libs.json.JsObject
import org.scalatest.Matchers._

@DoNotDiscover class UserMetadataServiceTests(val config: Map[String, String]) extends IntegrationTestCore {

  test("add/get user metadata fields") {
    //altitude.service.library.add(makeAsset(altitude.service.folder.getUserUncatFolder()))
    val metadataField = altitude.service.userMetadata.addField(
      MetadataField(userId = CURRENT_USER_ID, name = "fieldName", fieldType = "type"))

    val storedMetadataField = altitude.service.userMetadata.getFieldByName(metadataField.name)

    storedMetadataField should not be None
    storedMetadataField.get.id should equal(metadataField.id)
  }
}

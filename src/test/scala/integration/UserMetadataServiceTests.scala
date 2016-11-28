package integration

import altitude.models._
import org.scalatest.DoNotDiscover
import org.scalatest.Matchers._
import play.api.libs.json.JsObject
import org.scalatest.Matchers._

@DoNotDiscover class UserMetadataServiceTests(val config: Map[String, String]) extends IntegrationTestCore {

  test("add/get user metadata fields") {
    //altitude.service.library.add(makeAsset(altitude.service.folder.getUserUncatFolder()))
    SET_USER_1()
    val metadataField = altitude.service.userMetadata.addField(
      MetadataField(userId = CURRENT_USER_ID, name = "fieldName", fieldType = "type"))

    SET_USER_2()
    altitude.service.userMetadata.addField(
      MetadataField(userId = CURRENT_USER_ID, name = "fieldName", fieldType = "type"))

    SET_USER_1()
    altitude.service.userMetadata.getFieldByName(metadataField.name.toLowerCase) should not be None
    altitude.service.userMetadata.getFieldByName(metadataField.name.toUpperCase) should not be None
    altitude.service.userMetadata.getFieldById(metadataField.id.get) should not be None
  }

  test("get all user metadata fields") {
    altitude.service.userMetadata.addField(
      MetadataField(userId = CURRENT_USER_ID, name = "fieldName1", fieldType = "typ1"))
    altitude.service.userMetadata.addField(
      MetadataField(userId = CURRENT_USER_ID, name = "fieldName2", fieldType = "type2"))

    SET_USER_2()
    altitude.service.userMetadata.addField(
      MetadataField(userId = CURRENT_USER_ID, name = "fieldName", fieldType = "type"))

    SET_USER_1()
    altitude.service.userMetadata.getAllFields.length should be(2)

    SET_USER_2()
    altitude.service.userMetadata.getAllFields.length should be(1)
  }

  test("delete user metadata field") {
    val metadataField = altitude.service.userMetadata.addField(
      MetadataField(userId = CURRENT_USER_ID, name = "fieldName", fieldType = "type"))

    altitude.service.userMetadata.getAllFields.length should be(1)
    altitude.service.userMetadata.deleteFieldById(metadataField.id.get)
    altitude.service.userMetadata.getAllFields shouldBe empty
  }
}

package integration

import altitude.exceptions.ValidationException
import altitude.models._
import org.scalatest.DoNotDiscover
import org.scalatest.Matchers._

@DoNotDiscover class UserMetadataServiceTests(val config: Map[String, String]) extends IntegrationTestCore {

  test("add/get user metadata fields") {
    SET_USER_1()
    val metadataField = altitude.service.userMetadata.addField(
      UserMetadataField(userId = CURRENT_USER_ID, name = "field name", fieldType = FieldType.STRING.toString))

    SET_USER_2()
    altitude.service.userMetadata.addField(
      UserMetadataField(userId = CURRENT_USER_ID, name = "field name", fieldType = FieldType.NUMBER.toString))

    SET_USER_1()
    altitude.service.userMetadata.getFieldByName(metadataField.name.toLowerCase) should not be None
    altitude.service.userMetadata.getFieldByName(metadataField.name.toUpperCase) should not be None
    altitude.service.userMetadata.getFieldById(metadataField.id.get) should not be None
  }

  test("get all user metadata fields") {
    altitude.service.userMetadata.addField(
      UserMetadataField(userId = CURRENT_USER_ID, name = "field name 1", fieldType = FieldType.STRING.toString))
    altitude.service.userMetadata.addField(
      UserMetadataField(userId = CURRENT_USER_ID, name = "field name 2", fieldType = FieldType.STRING.toString))

    SET_USER_2()
    altitude.service.userMetadata.addField(
      UserMetadataField(userId = CURRENT_USER_ID, name = "field name 1", fieldType = FieldType.STRING.toString))

    SET_USER_1()
    altitude.service.userMetadata.getAllFields.length should be(2)

    SET_USER_2()
    altitude.service.userMetadata.getAllFields.length should be(1)
  }

  test("add/get user fixed list field") {
    val metadataField = altitude.service.userMetadata.addField(
      UserMetadataField(
        userId = CURRENT_USER_ID,
        name = "field name",
        fieldType = FieldType.STRING.toString,
        fixedList = Some(List("one", "two", "three"))))

    val storedFieldOpt = altitude.service.userMetadata.getFieldById(metadataField.id.get)
    storedFieldOpt should not be None
    val storedField: UserMetadataField = storedFieldOpt.get
    storedField.fixedList should not be None
    storedField.fixedList.get.length should be(metadataField.fixedList.get.length)
  }

  test("delete user metadata field") {
    val metadataField = altitude.service.userMetadata.addField(
      UserMetadataField(userId = CURRENT_USER_ID, name = "fieldName", fieldType = FieldType.FLAG.toString))

    altitude.service.userMetadata.getAllFields.length should be(1)
    altitude.service.userMetadata.deleteFieldById(metadataField.id.get)
    altitude.service.userMetadata.getAllFields shouldBe empty
  }

  test("add invalid field type") {

    intercept[ValidationException] {
      altitude.service.userMetadata.addField(
        UserMetadataField(userId = CURRENT_USER_ID, name = "fieldName", fieldType = "SO_INVALID"))
    }
  }
}

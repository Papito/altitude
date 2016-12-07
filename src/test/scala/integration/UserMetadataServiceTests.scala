package integration

import altitude.exceptions.ValidationException
import altitude.models._
import org.scalatest.DoNotDiscover
import org.scalatest.Matchers._

@DoNotDiscover class UserMetadataServiceTests(val config: Map[String, String]) extends IntegrationTestCore {

  test("add constraint value") {
    val metadataField = altitude.service.userMetadata.addField(
      UserMetadataField(
        userId = CURRENT_USER_ID,
        name = "fieldName",
        fieldType = FieldType.NUMBER.toString))

    altitude.service.userMetadata.addConstraintValue(metadataField.id, "one")
  }

  /*
    test("delete user field") {
      val metadataField = altitude.service.userMetadata.addField(
        UserMetadataField(
          userId = CURRENT_USER_ID,
          name = "fieldName",
          fieldType = FieldType.STRING.toString,
          constraintList = Some(List("one", "two", "three"))))

      altitude.service.userMetadata.getAllFields.length should be(1)
      altitude.service.userMetadata.deleteFieldById(metadataField.id.get)
      altitude.service.userMetadata.getAllFields shouldBe empty
    }

    test("add/get user constraint list") {
      val metadataField = altitude.service.userMetadata.addField(
        UserMetadataField(
          userId = CURRENT_USER_ID,
          name = "field name",
          fieldType = FieldType.STRING.toString,
          constraintList = Some(List("one", "two", "three"))))

      val storedFieldOpt = altitude.service.userMetadata.getFieldById(metadataField.id.get)
      storedFieldOpt should not be None
      var storedField: UserMetadataField = storedFieldOpt.get
      storedField.constraintList should not be None
      storedField.constraintList.get.length should be(metadataField.constraintList.get.length)

      // get the same field by name (which triggers a multi-record query)
      val storedFieldOpt2 = altitude.service.userMetadata.getFieldByName(metadataField.name)
      storedFieldOpt2 should not be None
      storedField = storedFieldOpt2.get
      storedField.constraintList should not be None
      storedField.constraintList.get.length should be(metadataField.constraintList.get.length)
    }

    test("add/get user fields") {
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

    test("get all user fields") {
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

    test("add invalid field type") {
      intercept[ValidationException] {
        altitude.service.userMetadata.addField(
          UserMetadataField(userId = CURRENT_USER_ID, name = "fieldName", fieldType = "SO_INVALID"))
      }
    }
  */
}

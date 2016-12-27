package integration

import altitude.exceptions.ValidationException
import altitude.models._
import org.scalatest.DoNotDiscover
import org.scalatest.Matchers._

@DoNotDiscover class UserMetadataServiceTests(val config: Map[String, String]) extends IntegrationTestCore {

  test("add/delete constraint value") {
    val metadataField = altitude.service.userMetadata.addField(
      UserMetadataField(
        name = "fieldName",
        fieldType = FieldType.NUMBER.toString))

    altitude.service.userMetadata.addConstraintValue(metadataField.id.get, "one")
    altitude.service.userMetadata.addConstraintValue(metadataField.id.get, "two")
    altitude.service.userMetadata.addConstraintValue(metadataField.id.get, "three")
    // test proper value binding
    altitude.service.userMetadata.addConstraintValue(metadataField.id.get, "\"LOL\"")
    var updatedField: UserMetadataField = altitude.service.userMetadata.getFieldById(metadataField.id.get).get
    updatedField.constraintList should not be None
    updatedField.constraintList.get.size shouldBe 4
    updatedField.constraintList.get.head shouldEqual "\"LOL\""

    altitude.service.userMetadata.deleteConstraintValue(metadataField.id.get, "two")
    updatedField = altitude.service.userMetadata.getFieldById(metadataField.id.get).get
    updatedField.constraintList.get.size shouldBe 3
    updatedField.constraintList.get.head shouldEqual "\"LOL\""

    altitude.service.userMetadata.deleteConstraintValue(metadataField.id.get, "\"LOL\"")
    updatedField = altitude.service.userMetadata.getFieldById(metadataField.id.get).get
    updatedField.constraintList.get.size shouldBe 2
    updatedField.constraintList.get.head shouldEqual "one"

    altitude.service.userMetadata.deleteConstraintValue(metadataField.id.get, "one")
    altitude.service.userMetadata.deleteConstraintValue(metadataField.id.get, "three")

    updatedField = altitude.service.userMetadata.getFieldById(metadataField.id.get).get
    updatedField.constraintList shouldBe empty
  }

  test("delete field") {
    val metadataField = altitude.service.userMetadata.addField(
      UserMetadataField(
        name = "fieldName",
        fieldType = FieldType.STRING.toString,
        constraintList = Some(List("one", "two", "three"))))

    altitude.service.userMetadata.getAllFields.length shouldBe 1
    altitude.service.userMetadata.deleteFieldById(metadataField.id.get)
    altitude.service.userMetadata.getAllFields shouldBe empty
  }

  test("add/get constraint list") {
    val metadataField = altitude.service.userMetadata.addField(
      UserMetadataField(
        name = "field name",
        fieldType = FieldType.STRING.toString,
        constraintList = Some(List("one", "two", "three"))))

    val storedFieldOpt = altitude.service.userMetadata.getFieldById(metadataField.id.get)
    storedFieldOpt should not be None
    var storedField: UserMetadataField = storedFieldOpt.get
    storedField.constraintList should not be None
    storedField.constraintList.get.length shouldBe metadataField.constraintList.get.length

    // get the same field by name (which triggers a multi-record query)
    val storedFieldOpt2 = altitude.service.userMetadata.getFieldById(metadataField.id.get)
    storedFieldOpt2 should not be None
    storedField = storedFieldOpt2.get
    storedField.constraintList should not be None
    storedField.constraintList.get.length shouldBe metadataField.constraintList.get.length
  }

  test("add/get fields") {
    val metadataField = altitude.service.userMetadata.addField(
      UserMetadataField(name = "field name", fieldType = FieldType.STRING.toString))

    altitude.service.userMetadata.getFieldById(metadataField.id.get) should not be None
  }

  test("get all fields") {
    altitude.service.userMetadata.addField(
      UserMetadataField(name = "field name 1", fieldType = FieldType.STRING.toString))
    altitude.service.userMetadata.addField(
      UserMetadataField(name = "field name 2", fieldType = FieldType.STRING.toString))

    SET_SECONDARY_USER()
    altitude.service.userMetadata.addField(
      UserMetadataField(name = "field name 3", fieldType = FieldType.STRING.toString))

    SET_PRIMARY_USER()
    altitude.service.userMetadata.getAllFields.length shouldBe 3

    SET_SECONDARY_USER()
    altitude.service.userMetadata.getAllFields.length shouldBe 3
  }

  test("add invalid field type") {
    intercept[ValidationException] {
        altitude.service.userMetadata.addField(
          UserMetadataField(name = "fieldName", fieldType = "SO_INVALID"))
      }
  }
}

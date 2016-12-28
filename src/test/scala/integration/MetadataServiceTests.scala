package integration

import altitude.exceptions.ValidationException
import altitude.models._
import org.scalatest.DoNotDiscover
import org.scalatest.Matchers._

@DoNotDiscover class MetadataServiceTests(val config: Map[String, String]) extends IntegrationTestCore {

/*
    test("hygiene and validation") {
      val metadataField = altitude.service.userMetadata.addField(
        UserMetadataField(
          name = "string field",
          fieldType = FieldType.NUMBER.toString))

      altitude.service.userMetadata.addConstraintValue(metadataField.id.get, "one")

      // everything should be lowercased
      intercept[DuplicateException] {
        altitude.service.userMetadata.addConstraintValue(metadataField.id.get, "ONE")
      }

      // test for trimmed space characters
      altitude.service.userMetadata.addConstraintValue(metadataField.id.get, "  Two     \t   Three  \n \t \r\n   Four ")

      // no empty values allowed
      intercept[ValidationException] {
        altitude.service.userMetadata.addConstraintValue(metadataField.id.get, "\r\n")
      }

      val updatedField: UserMetadataField = altitude.service.userMetadata.getFieldById(metadataField.id.get).get

      updatedField.constraintList.get should contain("one")
      updatedField.constraintList.get should contain("two three four")
    }

    test("constraint value field rules") {
      val metadataField = altitude.service.userMetadata.addField(
        UserMetadataField(
          name = "String field name",
          maxLength = Some(5),
          fieldType = FieldType.NUMBER.toString))

      intercept[ValidationException] {
        altitude.service.userMetadata.addConstraintValue(metadataField.id.get, "very very long")
      }
    }
*/

    test("delete field") {
      val metadataField = altitude.service.metadata.addField(
        MetadataField(
          name = "fieldName",
          fieldType = FieldType.STRING.toString))

      altitude.service.metadata.getAllFields.length shouldBe 1
      altitude.service.metadata.deleteFieldById(metadataField.id.get)
      altitude.service.metadata.getAllFields shouldBe empty
    }

    test("add/get fields") {
      val metadataField = altitude.service.metadata.addField(
        MetadataField(name = "field name", fieldType = FieldType.STRING.toString))

      altitude.service.metadata.getFieldById(metadataField.id.get) should not be None
    }

    test("get all fields") {
      altitude.service.metadata.addField(
        MetadataField(name = "field name 1", fieldType = FieldType.STRING.toString))
      altitude.service.metadata.addField(
        MetadataField(name = "field name 2", fieldType = FieldType.STRING.toString))

      SET_SECONDARY_USER()
      altitude.service.metadata.addField(
        MetadataField(name = "field name 3", fieldType = FieldType.STRING.toString))

      SET_PRIMARY_USER()
      altitude.service.metadata.getAllFields.length shouldBe 3

      SET_SECONDARY_USER()
      altitude.service.metadata.getAllFields.length shouldBe 3
    }

    test("add invalid field type") {
      intercept[ValidationException] {
          altitude.service.metadata.addField(
            MetadataField(name = "fieldName", fieldType = "SO_INVALID"))
        }
    }
}

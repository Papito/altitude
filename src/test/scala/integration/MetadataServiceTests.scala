package integration

import altitude.exceptions.ValidationException
import altitude.models._
import org.scalatest.DoNotDiscover
import org.scalatest.Matchers._

@DoNotDiscover class MetadataServiceTests(val config: Map[String, String]) extends IntegrationTestCore {

  test("add values") {
    val metadataField = altitude.service.metadata.addField(
      MetadataField(
        name = "string field",
        fieldType = FieldType.NUMBER.toString))

    val asset: Asset = altitude.service.library.add(makeAsset(altitude.service.folder.getUncatFolder))

    altitude.service.metadata.addValues(metadataField.id.get, asset.id.get, "one", "two", "three", "  \r\n \t ")
  }

/*
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
*/
}

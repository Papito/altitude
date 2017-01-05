package integration

import altitude.exceptions.{DuplicateException, NotFoundException, ValidationException}
import altitude.models._
import org.scalatest.DoNotDiscover
import org.scalatest.Matchers._

import scala.collection.immutable.HashMap


@DoNotDiscover class MetadataServiceTests(val config: Map[String, String]) extends IntegrationTestCore {

  test("set metadata values") {
    val keywordMetadataField = altitude.service.metadata.addField(
      MetadataField(
        name = "keyword field",
        // FIXME: use the enumeration
        fieldType = FieldType.KEYWORD))

    val numberMetadataField = altitude.service.metadata.addField(
      MetadataField(
        name = "number field",
        fieldType = FieldType.NUMBER))

    val asset: Asset = altitude.service.library.add(makeAsset(altitude.service.folder.getUncatFolder))

    // add a field we do not expect
    val badData = Map[String, Set[String]](
      keywordMetadataField.id.get -> Set("one", "two", "three"),
      BaseModel.genId -> Set("four"))

    intercept[NotFoundException] {
      altitude.service.metadata.setMetadata(asset.id.get, new Metadata(badData))
    }

    // duplicate values passed in
    val duplicateData = Map[String, Set[String]](
      keywordMetadataField.id.get -> Set("one", "One", "ONE"))

    intercept[ValidationException] {
      altitude.service.metadata.setMetadata(asset.id.get, new Metadata(duplicateData))
    }

    // valid
    val data = Map[String, Set[String]](
      keywordMetadataField.id.get -> Set("one", "two", "three"),
      numberMetadataField.id.get -> Set("1", "2", "3.002", "14.1", "1.25", "123456789"))

    altitude.service.metadata.setMetadata(asset.id.get, new Metadata(data))

    val storedMetadata = altitude.service.metadata.getMetadata(asset.id.get)

    storedMetadata.data should not be empty
    storedMetadata.data.keys should contain(keywordMetadataField.id.get)
    storedMetadata.data.keys should contain(numberMetadataField.id.get)
  }

  /*
    test("add/get fields") {
      val metadataField = altitude.service.metadata.addField(
        MetadataField(name = "field name", fieldType = FieldType.KEYWORD))

      val storedField = altitude.service.metadata.getFieldById(metadataField.id.get)
      storedField  should not be None
      (storedField.get: MetadataField).fieldType shouldBe FieldType.KEYWORD
    }

    test("update metadata values") {

    }

    test("delete field") {
      val metadataField = altitude.service.metadata.addField(
        MetadataField(
          name = "fieldName",
          fieldType = FieldType.STRING))

      altitude.service.metadata.getAllFields.length shouldBe 1
      altitude.service.metadata.deleteFieldById(metadataField.id.get)
      altitude.service.metadata.getAllFields shouldBe empty
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

    test("add duplicate field") {
      val fieldName = "field name"
      altitude.service.metadata.addField(
        MetadataField(name = fieldName, fieldType = FieldType.KEYWORD.toString))

      intercept[DuplicateException] {
        altitude.service.metadata.addField(
          MetadataField(name = fieldName, fieldType = FieldType.KEYWORD.toString))
      }
    }
  */
}

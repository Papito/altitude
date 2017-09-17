package software.altitude.test.core.integration

import org.scalatest.DoNotDiscover
import org.scalatest.Matchers._
import software.altitude.core.models._
import software.altitude.core.{DuplicateException, NotFoundException, ValidationException}


@DoNotDiscover class MetadataServiceTests(val config: Map[String, Any]) extends IntegrationTestCore {

  test("Number field type can be added") {
    val field = altitude.service.metadata.addField(
      MetadataField(
        name = "number field",
        fieldType = FieldType.NUMBER))
    val asset: Asset = altitude.service.library.add(makeAsset(altitude.service.folder.triageFolder))

    var data = Map[String, Set[String]](field.id.get -> Set("one"))
    intercept[ValidationException] {
      altitude.service.metadata.setMetadata(asset.id.get, new Metadata(data))
    }

    data = Map[String, Set[String]](field.id.get -> Set("."))
    intercept[ValidationException] {
      altitude.service.metadata.setMetadata(asset.id.get, new Metadata(data))
    }

    // these should be ok
    data = Map[String, Set[String]](
      field.id.get -> Set("000.", "0", "", "0000.00123", ".000", "36352424", "234324221"))
  }

  test("Boolean field type can be added") {
    val field = altitude.service.metadata.addField(
      MetadataField(
        name = "boolean field",
        fieldType = FieldType.BOOL))
    val asset: Asset = altitude.service.library.add(makeAsset(altitude.service.folder.triageFolder))

    var data = Map[String, Set[String]](field.id.get -> Set("one"))
    intercept[ValidationException] {
      altitude.service.metadata.setMetadata(asset.id.get, new Metadata(data))
    }

    data = Map[String, Set[String]](field.id.get -> Set("on"))
    intercept[ValidationException] {
      altitude.service.metadata.setMetadata(asset.id.get, new Metadata(data))
    }

    // cannot have conflicting boolean values
    data = Map[String, Set[String]](field.id.get -> Set("TRUE", "FALSE"))
    intercept[ValidationException] {
      altitude.service.metadata.setMetadata(asset.id.get, new Metadata(data))
    }
    // ... but non-conflicting duplicates are ok
    data = Map[String, Set[String]](field.id.get -> Set("TRUE", "TRUE"))

    // these should be ok
    data = Map[String, Set[String]](field.id.get -> Set("TRUE"))
    altitude.service.metadata.setMetadata(asset.id.get, new Metadata(data))

    data = Map[String, Set[String]](field.id.get -> Set("FALSE"))
    altitude.service.metadata.setMetadata(asset.id.get, new Metadata(data))

    data = Map[String, Set[String]](field.id.get -> Set("true"))
    altitude.service.metadata.setMetadata(asset.id.get, new Metadata(data))

    data = Map[String, Set[String]](field.id.get -> Set("False"))
    altitude.service.metadata.setMetadata(asset.id.get, new Metadata(data))
  }

  test("Setting metadata values") {
    val keywordMetadataField = altitude.service.metadata.addField(
      MetadataField(
        name = "keyword field",
        fieldType = FieldType.KEYWORD))

    val numberMetadataField = altitude.service.metadata.addField(
      MetadataField(
        name = "number field",
        fieldType = FieldType.NUMBER))

    val asset: Asset = altitude.service.library.add(makeAsset(altitude.service.folder.triageFolder))

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

  test("Test/update empty value sets") {
    val field1 = altitude.service.metadata.addField(
      MetadataField(
        name = "field 1",
        fieldType = FieldType.KEYWORD))

    val field2 = altitude.service.metadata.addField(
      MetadataField(
        name = "field 2",
        fieldType = FieldType.NUMBER))

    val asset: Asset = altitude.service.library.add(makeAsset(altitude.service.folder.triageFolder))

    var data = Map[String, Set[String]](
      field1.id.get -> Set("one", "two", "three"),
      field2.id.get -> Set())

    altitude.service.metadata.setMetadata(asset.id.get, new Metadata(data))

    var storedMetadata = altitude.service.metadata.getMetadata(asset.id.get)
    storedMetadata.data.keys should contain(field1.id.get)
    storedMetadata.data.keys shouldNot contain(field2.id.get)

    // update with nothing
    data = Map[String, Set[String]](field1.id.get -> Set())

    altitude.service.metadata.updateMetadata(asset.id.get, new Metadata(data))

    storedMetadata = altitude.service.metadata.getMetadata(asset.id.get)
    storedMetadata.data shouldBe empty
  }

  test("Update metadata values") {
    val field1 = altitude.service.metadata.addField(
      MetadataField(
        name = "field 1",
        fieldType = FieldType.KEYWORD))

    val field2 = altitude.service.metadata.addField(
      MetadataField(
        name = "field 2",
        fieldType = FieldType.NUMBER))

    val asset: Asset = altitude.service.library.add(makeAsset(altitude.service.folder.triageFolder))

    var data = Map[String, Set[String]](
        field1.id.get -> Set("one", "two", "three"),
        field2.id.get -> Set("1", "2", "3.002", "14.1", "1.25", "123456789"))

    altitude.service.metadata.setMetadata(asset.id.get, new Metadata(data))

    var storedMetadata = altitude.service.metadata.getMetadata(asset.id.get)
    storedMetadata.data.keys should contain(field1.id.get)
    storedMetadata.data.keys should contain(field2.id.get)

    val field3 = altitude.service.metadata.addField(
      MetadataField(
        name = "field 3",
        fieldType = FieldType.KEYWORD))

    data = Map[String, Set[String]](
        field3.id.get -> Set("test 1", "test 2"),
        field2.id.get -> Set("3.002", "14.1", "1.25", "123456789"))

    altitude.service.metadata.updateMetadata(asset.id.get, new Metadata(data))

    storedMetadata = altitude.service.metadata.getMetadata(asset.id.get)
    storedMetadata.data.keys should contain(field1.id.get)
    storedMetadata.data.keys should contain(field2.id.get)
    storedMetadata.data.keys should contain(field3.id.get)

    storedMetadata.data(field2.id.get) shouldNot contain("1")
    storedMetadata.data(field2.id.get) shouldNot contain("2")
  }

  test("Add/get fields") {
    val metadataField = altitude.service.metadata.addField(
      MetadataField(name = "field name", fieldType = FieldType.KEYWORD))

    val storedField: MetadataField = altitude.service.metadata.getFieldById(metadataField.id.get)
    storedField.fieldType shouldBe FieldType.KEYWORD
  }

  test("Delete metadata field") {
    val metadataField = altitude.service.metadata.addField(
      MetadataField(
        name = "fieldName",
        fieldType = FieldType.KEYWORD))

    altitude.service.metadata.getFieldById(metadataField.id.get)

    altitude.service.metadata.deleteFieldById(metadataField.id.get)

    intercept[NotFoundException] {
      altitude.service.metadata.getFieldById(metadataField.id.get)
    }
  }

  test("Get all fields for a repo") {
    altitude.service.metadata.addField(
      MetadataField(name = "field name 1", fieldType = FieldType.KEYWORD))
    altitude.service.metadata.addField(
      MetadataField(name = "field name 2", fieldType = FieldType.KEYWORD))

    SET_SECOND_USER()
    altitude.service.metadata.addField(
      MetadataField(name = "field name 3", fieldType = FieldType.KEYWORD))

    SET_FIRST_USER()
    altitude.service.metadata.getAllFields.size shouldBe 3

    SET_SECOND_USER()
    altitude.service.metadata.getAllFields.size shouldBe 3
  }

  test("Adding a duplicate-named field should not succeed") {
    val fieldName = "field name"
    altitude.service.metadata.addField(
      MetadataField(name = fieldName, fieldType = FieldType.KEYWORD))

    intercept[DuplicateException] {
          altitude.service.metadata.addField(
            MetadataField(name = fieldName, fieldType = FieldType.KEYWORD))
        }
  }

  test("Sanitizing field names") {
    val metadataField = altitude.service.metadata.addField(
      MetadataField(name = " new field\n ", fieldType = FieldType.NUMBER))

    metadataField.name shouldBe "new field"
  }

  test("Failing validation cases") {
    intercept[ValidationException] {
          altitude.service.metadata.addField(
            MetadataField(name = "  ", fieldType = FieldType.KEYWORD))
    }

    intercept[ValidationException] {
          altitude.service.metadata.addField(
            MetadataField(name = "\t\n\r   ", fieldType = FieldType.KEYWORD))
    }

    intercept[ValidationException] {
          altitude.service.metadata.addField(
            MetadataField(name = "", fieldType = FieldType.KEYWORD))
    }
  }

  test("Metadata added initially should be present") {
    val field = altitude.service.metadata.addField(
      MetadataField(
        name = "keyword field",
        fieldType = FieldType.KEYWORD))

    val data = Map[String, Set[String]](field.id.get -> Set("one", "two"))
    val metadata = new Metadata(data)

    val asset: Asset = altitude.service.library.add(
      makeAsset(altitude.service.folder.triageFolder, metadata = metadata))

    val storedAsset: Asset = altitude.service.library.getById(asset.id.get)

    storedAsset.metadata.isEmpty shouldBe false
  }

  test("Not defined user metadata values should not return") {
    altitude.service.metadata.addField(
      MetadataField(
        name = "keyword field",
        fieldType = FieldType.KEYWORD))

    altitude.service.metadata.addField(
      MetadataField(
        name = "number field",
        fieldType = FieldType.NUMBER))

    val field3 = altitude.service.metadata.addField(
      MetadataField(
        name = "text field",
        fieldType = FieldType.TEXT))


    val data = Map[String, Set[String]](field3.id.get -> Set("this is some text"))
    val metadata = new Metadata(data)

    val asset: Asset = altitude.service.library.add(
      makeAsset(altitude.service.folder.triageFolder, metadata = metadata))

    val storedAsset: Asset = altitude.service.library.getById(asset.id.get)

    storedAsset.metadata.isEmpty shouldBe false
    storedAsset.metadata.data.size shouldBe 1
  }
}

package integration

import altitude.models._
import org.scalatest.DoNotDiscover

@DoNotDiscover class SearchServiceTests(val config: Map[String, String]) extends IntegrationTestCore {

  test("index and search assets") {
    val field1 = altitude.service.metadata.addField(
      MetadataField(
        name = "field 1",
        fieldType = FieldType.KEYWORD))

    val field2 = altitude.service.metadata.addField(
      MetadataField(
        name = "field 2",
        fieldType = FieldType.NUMBER))

    val data = Map[String, Set[String]](
        field1.id.get -> Set("one", "two", "three"),
        field2.id.get -> Set("1", "2", "3.002", "14.1", "1.25", "123456789"))
    val assetData1 = makeAsset(altitude.service.folder.getUncatFolder, new Metadata(data))
    val asset1: Asset = altitude.service.library.add(assetData1)
  }
}

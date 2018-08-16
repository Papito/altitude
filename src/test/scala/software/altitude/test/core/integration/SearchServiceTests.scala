package software.altitude.test.core.integration

import org.scalatest.DoNotDiscover
import org.scalatest.Matchers._
import software.altitude.core.models._
import software.altitude.core.util.{Query, QueryResult, SearchQuery}

@DoNotDiscover class SearchServiceTests(val config: Map[String, Any]) extends IntegrationTestCore {

  test("Index and search by term") {
    val field1 = altitude.service.metadata.addField(
      MetadataField(
        name = "keywords",
        fieldType = FieldType.KEYWORD))

    val field2 = altitude.service.metadata.addField(
      MetadataField(
        name = "quotes",
        fieldType = FieldType.TEXT))

    val field3 = altitude.service.metadata.addField(
      MetadataField(
        name = "cast",
        fieldType = FieldType.KEYWORD))

    var data = Map[String, Set[String]](
      field1.id.get -> Set("picture", "man", "office", "monday", "how is this my life?"),
      field2.id.get -> Set(
        """
          We have blueberry, raspberry, ginseng, sleepy time, green tea,
          green tea with lemon, green tea with lemon and honey, liver disaster,
          ginger with honey, ginger without honey, vanilla almond, white truffel,
          blueberry chamomile, vanilla walnut, constant comment and... earl grey.
          """,
        """
          Ok this next song goes out to the guy who keeps yelling from the balcony.
          It's called "We Hate You, Please Die."
          """,
        """
          I partake not in the meat, nor the breast milk, nor the ovum, of any creature, with a face.
          """
      ),
      field3.id.get -> Set("Lindsay Lohan", "Conan O'Brien", "Teri Hatcher", "Sam Rockwell"))

    val assetData1 = makeAsset(altitude.service.folder.triageFolder, Metadata(data))
    altitude.service.library.add(assetData1)

    // scalastyle:off
    data = Map[String, Set[String]](
      field1.id.get -> Set("tree", "shoe", "desert", "California"),
      field2.id.get -> Set(
        """
          “If I ever start referring to these as the best years of my life — remind me to kill myself.”
          """,
        """
          George Washington was in a cult, and that cult was into aliens, man.
          """,
        """
          I’d like to stop thinking of the present as some minor, insignificant preamble to something else.
          """
      ),
      field3.id.get -> Set("Keanu Reeves", "Sandra Bullock", "Dennis Hopper", "Teri Hatcher"))
    // scalastyle:on

    val assetData2 = makeAsset(altitude.service.folder.triageFolder, Metadata(data))
    altitude.service.library.add(assetData2)

    var results: QueryResult = altitude.service.library.search(new SearchQuery(text = Some("keanu")))
    results.nonEmpty shouldBe true
    results.total shouldBe 1
    // check that the document is indeed - an asset
    val resultJson = results.records.head
    Asset.fromJson(resultJson)

    results = altitude.service.library.search(new SearchQuery(text = Some("TERI")))
    results.nonEmpty shouldBe true
    results.total shouldBe 2
  }

  test("Narrow down search to a folder") {
    val field1 = altitude.service.metadata.addField(
      MetadataField(
        name = "keywords",
        fieldType = FieldType.KEYWORD))

    val data = Map[String, Set[String]](
      field1.id.get -> Set("space", "force", "tactical", "pants")
    )

    val metadata = Metadata(data)

    val folder1: Folder = altitude.service.library.addFolder("folder1")

    val folder1_1: Folder = altitude.service.library.addFolder(
      name = "folder1_1", parentId = folder1.id)

    1 to 3 foreach {_ =>
      altitude.service.library.add(makeAsset(folder1_1, metadata))
    }
    1 to 3 foreach {_ =>
      altitude.service.library.add(makeAsset(folder1, metadata))
    }

    val qFolder1_1 = new SearchQuery(text = Some("space"), folderIds = Set(folder1_1.id.get))
    var results: QueryResult = altitude.service.library.search(qFolder1_1)
    results.total shouldBe 3

    val qFolder1 = new SearchQuery(text = Some("space"), folderIds = Set(folder1.id.get))
    results = altitude.service.library.search(qFolder1)
    results.total shouldBe 6

    val qAllFolders = new SearchQuery()
    results = altitude.service.library.search(qAllFolders)
    results.total shouldBe 6

  }

  test("Create assets and search by metadata") {
    val field1 = altitude.service.metadata.addField(
      MetadataField(
        name = "field 1",
        fieldType = FieldType.KEYWORD))

    val field2 = altitude.service.metadata.addField(
      MetadataField(
        name = "field 2",
        fieldType = FieldType.NUMBER))

    val field3 = altitude.service.metadata.addField(
      MetadataField(
        name = "field 3",
        fieldType = FieldType.BOOL))

    var data = Map[String, Set[String]](
      field1.id.get -> Set("one", "two", "three"),
      field2.id.get -> Set("1", "2", "3.002", "14.1", "1.25", "123456789"),
      field3.id.get -> Set("true"))
    val assetData1 = makeAsset(altitude.service.folder.triageFolder, Metadata(data))
    altitude.service.library.add(assetData1)

    data = Map[String, Set[String]](
      field1.id.get -> Set("six", "seven"),
      field2.id.get -> Set("5", "1001", "1"),
      field3.id.get -> Set("true"))
    val assetData2 = makeAsset(altitude.service.folder.triageFolder, Metadata(data))
    altitude.service.library.add(assetData2)

    // simple value search
    var results = altitude.service.library.search(new SearchQuery(text = Some("one")))
    results.total shouldBe 1

    val keywordSearchParam = Query.EQUALS(1)
    results = altitude.service.library.search(
      new SearchQuery(params = Map(field1.id.get -> keywordSearchParam))
    )
    results.total shouldBe 1
  }

  test("Tag assets and search by metadata", Focused) {
    val field1 = altitude.service.metadata.addField(
      MetadataField(
        name = "field 1",
        fieldType = FieldType.KEYWORD))

    val field2 = altitude.service.metadata.addField(
      MetadataField(
        name = "field 2",
        fieldType = FieldType.NUMBER))

    val field3 = altitude.service.metadata.addField(
      MetadataField(
        name = "field 3",
        fieldType = FieldType.BOOL))

    val asset1: Asset = altitude.service.library.add(
      makeAsset(altitude.service.folder.triageFolder))

    altitude.service.library.addMetadataValue(asset1.id.get, fieldId = field1.id.get, newValue = "one")

    var results = altitude.service.library.search(new SearchQuery(text = Some("one")))
    results.total shouldBe 1
  }

  test("Recycled assets should not be in the search index") {
    val asset: Asset = altitude.service.library.add(makeAsset(altitude.service.folder.triageFolder))
    altitude.service.library.add(makeAsset(altitude.service.folder.triageFolder))

    altitude.service.library.recycleAsset(asset.id.get)

    val results = altitude.service.library.search(new SearchQuery)
    results.total shouldBe 1
  }
}

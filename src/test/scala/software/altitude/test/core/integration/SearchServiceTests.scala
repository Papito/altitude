package software.altitude.test.core.integration

import org.scalatest.DoNotDiscover
import org.scalatest.Matchers._
import software.altitude.core.Util
import software.altitude.core.models._
import software.altitude.core.util.{Query, QueryResult, SearchQuery}
import software.altitude.core.{Const => C}

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
          I partake not in the meat, nor the breas tmilk, nor the ovum, of any creature, with a face.
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

    var results: QueryResult = altitude.service.search.search(new SearchQuery(text = "keanu"))
    results.nonEmpty shouldBe true
    results.total shouldBe 1
    // check that the document is indeed - an asset
    val resultJson = results.records.head
    Asset.fromJson(resultJson)

    results = altitude.service.search.search(new SearchQuery(text = "TERI"))
    results.nonEmpty shouldBe true
    results.total shouldBe 2
  }

  test("Search by folder hierarchy should return assets in subfolders", Focused) {
    /*
  folder1
    folder1_1
    folder1_2
  */
    val folder1: Folder = altitude.service.library.addFolder("folder1")

    val folder1_1: Folder = altitude.service.library.addFolder(
      name = "folder1_1", parentId = folder1.id)

    folder1_1.parentId should not be None

    val folder1_2: Folder = altitude.service.library.addFolder(
      name = "folder1_2", parentId = folder1.id)

    val mediaType = new AssetType(mediaType = "mediaType", mediaSubtype = "mediaSubtype", mime = "mime")

    altitude.service.asset.add(new Asset(
      folderId = folder1_1.id.get.toString,
      userId = currentUser.id.get,
      assetType = mediaType,
      fileName = "filename.ext",
      path = Some(Util.randomStr(30)),
      checksum = Util.randomStr(32),
      sizeBytes = 1L))

    altitude.service.asset.add(new Asset(
      folderId = folder1_2.id.get.toString,
      userId = currentUser.id.get,
      assetType = mediaType,
      fileName = "filename.ext",
      path = Some(Util.randomStr(30)),
      checksum = Util.randomStr(32),
      sizeBytes = 1L))

    altitude.service.asset.add(new Asset(
      folderId = folder1.id.get.toString,
      userId = currentUser.id.get,
      assetType = mediaType,
      fileName = "filename.ext",
      path = Some(Util.randomStr(30)),
      checksum = Util.randomStr(32),
      sizeBytes = 1L))

    altitude.service.library.query(
      new Query(Map(C.Asset.FOLDER_ID -> folder1_2.id.get))
    ).records.length shouldBe 1

    altitude.service.library.query(
      new Query(Map(C.Asset.FOLDER_ID -> folder1_1.id.get))
    ).records.length shouldBe 1

    altitude.service.library.query(
      new Query(Map(C.Asset.FOLDER_ID -> folder1.id.get))
    ).records.length shouldBe 3
  }

  /*

  Same idea - parent folder will not get records from subfolders without search

  test("Folder filtering") {
    /*
    folder1
    folder2
      folder2_1
    */
    val folder1: Folder = altitude.service.library.addFolder("folder1")

    val folder2: Folder = altitude.service.library.addFolder("folder2")

    val folder2_1: Folder = altitude.service.library.addFolder(
      name = "folder2_1", parentId = folder2.id)

    // fill up the hierarchy with assets x times over
    1 to 2 foreach {n =>
      altitude.service.library.add(makeAsset(folder1))
      altitude.service.library.add(makeAsset(folder2))
      altitude.service.library.add(makeAsset(folder2_1))
    }

    altitude.service.library.query(
      new Query(folderIds = Set(folder1.id.get))
    ).records.length shouldBe 2

    altitude.service.library.query(
      new Query(folderIds = Set(folder2_1.id.get))
    ).records.length shouldBe 2

    altitude.service.library.query(
      new Query(folderIds = Set(folder2.id.get))
    ).records.length shouldBe 4
  }

*/



  /*
    test("index and search by metadata", focused) {
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
      val assetData1 = makeAsset(altitude.service.folder.getTriageFolder, new Metadata(data))
      val asset1: Asset = altitude.service.library.add(assetData1)

      data = Map[String, Set[String]](
          field1.id.get -> Set("six", "seven"),
          field2.id.get -> Set("5", "1001", "1"),
          field3.id.get -> Set("true"))
      val assetData2 = makeAsset(altitude.service.folder.getTriageFolder, new Metadata(data))
      val asset2: Asset = altitude.service.library.add(assetData2)
    }

    test("index and search by term and metadata") {
    }
  */
}

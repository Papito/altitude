package software.altitude.test.core.unit

import org.scalatest.FunSuite
import org.scalatest.Matchers._
import software.altitude.core.dao.postgres.querybuilder.{AssetSearchQueryBuilder => PostgresAssetSearchQueryBuilder}
import software.altitude.core.dao.sqlite.querybuilder.{AssetSearchQueryBuilder => SqliteAssetSearchQueryBuilder}
import software.altitude.core.models.Repository
import software.altitude.core.util.{Query, SearchQuery}
import software.altitude.core.{Context, Const => C}
import software.altitude.test.core.TestFocus

class SearchSqlQueryTests extends FunSuite with TestFocus {
  private val repo = new Repository(
    id = Some("1"),
    name = "repo name",
    rootFolderId = "1",
    triageFolderId = "2",
    fileStoreConfig = Map(),
    fileStoreType = C.FileStoreType.FS)

  implicit val ctx: Context = new Context(
    repo = repo,
    user = null
  )

  test("Basic WHERE SQL asset query is built correctly") {
    val builder = new SqliteAssetSearchQueryBuilder(List("*"))
    val q = new SearchQuery()
    val sqlQuery = builder.buildSelectSql(q)
    sqlQuery.sqlAsString shouldBe "SELECT * FROM asset WHERE asset.repository_id = ? AND is_recycled = ?"
    sqlQuery.bindValues.size shouldBe 2
  }

  test("WHERE asset query can be narrowed down by folders") {
    val builder = new SqliteAssetSearchQueryBuilder(List("*"))
    val q = new SearchQuery(folderIds = Set("1", "2", "3"))
    val sqlQuery = builder.buildSelectSql(q)
    sqlQuery.sqlAsString shouldBe "SELECT * FROM asset WHERE asset.repository_id = ? AND is_recycled = ? AND folder_id IN (?, ?, ?)"
    sqlQuery.bindValues.size shouldBe 5
  }

  test("Text Sqlite text search SQL query is built correctly") {
    val builder = new SqliteAssetSearchQueryBuilder(List("*"))
    val q = new SearchQuery(text = Some("my text"))
    val sqlQuery = builder.buildSelectSql(q)
    sqlQuery.sqlAsString shouldBe "SELECT * FROM asset, search_document WHERE asset.repository_id = ? AND search_document.repository_id = ? AND body MATCH ? AND is_recycled = ? AND search_document.asset_id = asset.id"
    sqlQuery.bindValues.size shouldBe 4
  }

  test("Text Postgres text search SQL query is built correctly") {
    val builder = new PostgresAssetSearchQueryBuilder(List("*"))
    val q = new SearchQuery(text = Some("my text"))
    val sqlQuery = builder.buildSelectSql(q)
    sqlQuery.sqlAsString shouldBe "SELECT * FROM asset, search_document WHERE asset.repository_id = ? AND search_document.repository_id = ? AND search_document.tsv @@ to_tsquery(?) AND is_recycled = ? AND search_document.asset_id = asset.id"
    sqlQuery.bindValues.size shouldBe 4
  }

  test("Parametarized asset search SQL is built correctly") {
    val builder = new PostgresAssetSearchQueryBuilder(List("*"))
    val q = new SearchQuery(
      params = Map(
        "text_field_id" -> "lol",
        "number_field_id" -> 12,
        "boolean_field_id" -> Query.EQUALS(false)
      )
    )
    val sqlQuery = builder.buildSelectSql(q)
    sqlQuery.sqlAsString shouldBe "SELECT * FROM asset, search_parameter WHERE asset.repository_id = ? AND search_parameter.repository_id = ? AND is_recycled = ? AND ((field_id = ? AND field_value_kw = ?) OR (field_id = ? AND field_value_num = ?) OR (field_id = ? AND field_value_bool = ?)) AND search_parameter.asset_id = asset.id GROUP BY asset.id HAVING count(asset.id) >= 3"
    sqlQuery.bindValues.size shouldBe 9
  }
}

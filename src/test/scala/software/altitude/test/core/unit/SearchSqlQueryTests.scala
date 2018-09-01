package software.altitude.test.core.unit

import org.scalatest.FunSuite
import org.scalatest.Matchers._
import software.altitude.core.dao.sqlite.querybuilder.{AssetSearchQueryBuilder => SqliteAssetSearchQueryBuilder}
import software.altitude.core.dao.sqlite.querybuilder.{AssetSearchQueryBuilder => PostgresAssetSearchQueryBuilder}
import software.altitude.core.{Const => C, Context}
import software.altitude.core.models.Repository
import software.altitude.core.util.{Query, SearchQuery}
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

  test("Basic WHERE SQL asset query is built correctly", Focused) {
    val builder = new SqliteAssetSearchQueryBuilder(List("col1"))
    val q = new SearchQuery()
    val sqlQuery = builder.buildSelectSql(q)
    sqlQuery.sqlAsString shouldBe s"SELECT col1 FROM asset WHERE asset.repository_id = ? AND is_recycled = ?"
    sqlQuery.bindValues.size shouldBe 2
  }

  test("WHERE asset query can be narrowed down by folders", Focused) {
    val builder = new SqliteAssetSearchQueryBuilder(List("col1"))
    val q = new SearchQuery(folderIds = Set("1", "2", "3"))
    val sqlQuery = builder.buildSelectSql(q)
    sqlQuery.sqlAsString shouldBe s"SELECT col1 FROM asset WHERE asset.repository_id = ? AND is_recycled = ? AND folder_id IN (?, ?, ?)"
    sqlQuery.bindValues.size shouldBe 5
  }

  test("Text search SQL query is built correctly", Focused) {
    val builder = new SqliteAssetSearchQueryBuilder(List("col1"))
    val q = new SearchQuery()
    val sqlQuery = builder.buildSelectSql(q)
    sqlQuery.sqlAsString shouldBe s"SELECT col1 FROM asset WHERE asset.repository_id = ? AND is_recycled = ?"
    sqlQuery.bindValues.size shouldBe 2
  }
}

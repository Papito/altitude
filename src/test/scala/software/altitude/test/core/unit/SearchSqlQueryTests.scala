package software.altitude.test.core.unit

import org.scalatest.FunSuite
import org.scalatest.Matchers._
import software.altitude.core.dao.sqlite.querybuilder.AssetSearchQueryBuilder
import software.altitude.core.{Const, Context}
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
    fileStoreType = Const.FileStoreType.FS)

  implicit val ctx: Context = new Context(
    repo = repo,
    user = null
  )

  test("Basic WHERE SQL asset query is built correctly") {
    val builder = new AssetSearchQueryBuilder(List("col1"), Set("table1"))
    val q = new SearchQuery(params = Map("searchValue" -> 3), folderIds = Set("1", "2", "3"))
    val sqlQuery = builder.buildSelectSql(q)
    sqlQuery.sqlAsString shouldBe "SELECT col1 FROM table1 WHERE searchValue = ? AND table1.repository_id = ?"
    sqlQuery.bindValues.size shouldBe 2

    // TODO: check recycle flag in the query
  }

  test("WHERE asset query can be narrowed down by folders", Focused) {
    val builder = new AssetSearchQueryBuilder(List("col1"), Set("table1"))
    val q = new SearchQuery(params = Map("searchValue" -> 3), folderIds = Set("1", "2", "3"))
    val sqlQuery = builder.buildSelectSql(q)
    sqlQuery.sqlAsString shouldBe "SELECT col1 FROM table1 WHERE searchValue = ? AND table1.repository_id = ?"
    sqlQuery.bindValues.size shouldBe 2
  }
}

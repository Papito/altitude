package software.altitude.core.dao.sqlite.querybuilder

import org.slf4j.LoggerFactory
import software.altitude.core.dao.jdbc.querybuilder.{SearchQueryBuilder, SqlQuery, SqlQueryBuilder}
import software.altitude.core.transactions.TransactionId
import software.altitude.core.util.SearchQuery
import software.altitude.core.{Context, Const => C}

class AssetSearchQueryBuilder(sqlColsForSelect: String, tableNames: Set[String])
  extends SqlQueryBuilder(sqlColsForSelect = sqlColsForSelect, tableNames = tableNames) with SearchQueryBuilder {
  private final val log = LoggerFactory.getLogger(getClass)

  def build(query: SearchQuery, countOnly: Boolean)
           (implicit ctx: Context, txId: TransactionId): SqlQuery = {
    val (whereClause, sqlBindVals) = compileQuery(query)

    val sql = if (countOnly) {
      assembleQuery(
        select = "count(*) AS count",
        from = tableNamesForSelect,
        where = whereClause)
    }
    else {
      assembleQuery(
        select = sqlColsForSelect,
        from = tableNamesForSelect,
        where = whereClause,
        rpp = query.rpp,
        page = query.page)
    }

    log.debug(s"SQL QUERY: $sql with $sqlBindVals")
    SqlQuery(sql, sqlBindVals)
  }

  protected def compileQuery(searchQuery: SearchQuery)(implicit ctx: Context): (String, List[Any]) = {
    val sQueryWithNoRecycled = searchQuery.add(C.Asset.IS_RECYCLED -> false)
    val queryTextBindVal = if (sQueryWithNoRecycled.text.isDefined) List(sQueryWithNoRecycled.text.get) else List()
    val sqlBindVals = getSqlBindVals(sQueryWithNoRecycled) ::: queryTextBindVal
    val queryTextWhereClause = if (sQueryWithNoRecycled.text.isDefined) List("body MATCH ?") else List()

    val whereClauses = getWhereClauses(sQueryWithNoRecycled) :::
      List(s"search_document.${C.SearchToken.ASSET_ID} = asset.id") :::
      queryTextWhereClause

    compileSearchQuery(searchQuery, whereClauses, sqlBindVals)
  }

}

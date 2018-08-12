package software.altitude.core.dao.sqlite.querybuilder

import org.slf4j.LoggerFactory
import software.altitude.core.dao.jdbc.querybuilder.{SqlQuery, SqlQueryBuilder}
import software.altitude.core.transactions.TransactionId
import software.altitude.core.util.SearchQuery
import software.altitude.core.{Context, Const => C}

class AssetSearchQueryBuilder(sqlColsForSelect: String, tableNames: Set[String])
  extends SqlQueryBuilder(sqlColsForSelect = sqlColsForSelect, tableNames = tableNames) {
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

  protected def compileQuery(query: SearchQuery)(implicit ctx: Context): (String, List[Any]) = {
    val sqlBindVals = getSqlBindVals(query) :+ (
      if (query.text.isDefined) query.text.get else None)

    val whereClauses = getWhereClauses(query) :+
      s"search_document.${C.SearchToken.ASSET_ID} = asset.id" :+ (
      // text match if there is a text query
      if (query.text.isDefined) s"body MATCH ?" else None)

    val whereClause = s"""WHERE ${whereClauses.filter(_ != None).mkString(" AND ")}"""

    (whereClause, sqlBindVals.filter(_ != None))
  }

}

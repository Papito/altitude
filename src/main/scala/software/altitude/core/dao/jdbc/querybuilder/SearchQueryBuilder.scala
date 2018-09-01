package software.altitude.core.dao.jdbc.querybuilder

import org.slf4j.LoggerFactory
import software.altitude.core.util.Query.QueryParam
import software.altitude.core.util.{Query, SearchQuery}
import software.altitude.core.{Context, Const => C}

/**
  * Common code for JDBC search query builders
  */
abstract class SearchQueryBuilder(sqlColsForSelect: List[String], tableNames: Set[String])
  extends SqlQueryBuilder(sqlColsForSelect, tableNames) {

  private final val log = LoggerFactory.getLogger(getClass)

  def buildSelectSql(searchQuery: SearchQuery)(implicit ctx: Context): SqlQuery = {
    val allClauses = compileClauses(searchQuery, ctx)

    val sql: String  = selectStr(allClauses) +
      fromStr(allClauses) +
      whereStr(allClauses) +
      limitStr(searchQuery) +
      offsetStr(searchQuery)

    val bindVals = allClauses.foldLeft(List[Any]()) { (res, comp) =>
      res ++ comp._2.bindVals
    }

    log.debug(s"Select SQL: $sql with $bindVals")
    SqlQuery(sql, bindVals)
  }

  def buildCountSql(searchQuery: SearchQuery)(implicit ctx: Context): SqlQuery = {
    // the SQL is the same but the WHERE clause is just the COUNT
    val whereClauseForCount = ClauseComponents(List("COUNT(*) AS count"))
    val allClauses = compileClauses(searchQuery, ctx) + (
      SqlQueryBuilder.WHERE -> whereClauseForCount)

    val sql: String  = selectStr(allClauses) + fromStr(allClauses) + whereStr(allClauses)

    val bindVals = allClauses.foldLeft(List[Any]()) { (res, comp) =>
      res ++ comp._2.bindVals
    }

    log.debug(s"Count SQL: $sql with $bindVals")
    SqlQuery(sql, bindVals)
  }

}

package software.altitude.core.dao.sqlite.querybuilder

import org.slf4j.LoggerFactory
import software.altitude.core.dao.jdbc.querybuilder.{SearchQueryBuilder, SqlQuery, SqlQueryBuilder}
import software.altitude.core.transactions.TransactionId
import software.altitude.core.util.Query.QueryParam
import software.altitude.core.util.{Query, SearchQuery}
import software.altitude.core.{Context, Const => C}

class AssetSearchQueryBuilder(sqlColsForSelect: String, tableNames: Set[String])
  extends SqlQueryBuilder(sqlColsForSelect = sqlColsForSelect, tableNames = tableNames) with SearchQueryBuilder {
  private final val log = LoggerFactory.getLogger(getClass)
  private val searchParamTable = "search_parameter"

  def build(searchQuery: SearchQuery, countOnly: Boolean)
           (implicit ctx: Context, txId: TransactionId): SqlQuery = {
    val (whereClause, sqlBindVals) = compileQuery(searchQuery)

    // add search_parameter to table names, if this is a parametarized search
    val _tableNamesForSelect = if (searchQuery.isParametarized) {
      s"$tableNamesForSelect, $searchParamTable"
    } else {
      tableNamesForSelect
    }

    val sql = if (countOnly) {
      assembleQuery(
        select = "count(*) AS count",
        from = _tableNamesForSelect,
        where = whereClause)
    }
    else {
      assembleQuery(
        select = sqlColsForSelect,
        from = _tableNamesForSelect,
        where = whereClause,
        rpp = searchQuery.rpp,
        page = searchQuery.page)
    }

    log.debug(s"SQL QUERY: $sql with $sqlBindVals")
    println(sql, sqlBindVals)
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

  protected def getWhereClauses(searchQuery: SearchQuery)(implicit ctx: Context): List[String] = {
    searchQuery.params.map { el: (String, Any) =>
      val (columnName, value) = el
      value match {
        case _: String => s"$columnName = ?"
        case _: Boolean => s"$columnName = ?"
        case _: Number => s"$columnName = ?"
        case qParam: QueryParam => qParam.paramType match {
          case Query.ParamType.IN => {
            val placeholders: String = qParam.values.toList.map {_ => "?"}.mkString(", ")
            s"$columnName IN ($placeholders)"
          }
          case Query.ParamType.EQ => s"$columnName = ?"

          case _ => throw new IllegalArgumentException(s"This type of parameter is not supported: ${qParam.paramType}")
        }
        case _ => throw new IllegalArgumentException(s"This type of parameter is not supported: $value")
      }
    }.toList ::: tableNames.map(tableName => s"$tableName.${C.Base.REPO_ID} = ?").toList
  }


}

package software.altitude.core.dao.sqlite.querybuilder

import org.slf4j.LoggerFactory
import software.altitude.core.dao.jdbc.querybuilder.{SearchQueryBuilder, SqlQuery, SqlQueryBuilder}
import software.altitude.core.transactions.TransactionId
import software.altitude.core.util.Query.QueryParam
import software.altitude.core.util.{Query, SearchQuery}
import software.altitude.core.{Context, Const => C}

class AssetSearchQueryBuilder(sqlColsForSelect: String, tableNames: Set[String])
  extends SqlQueryBuilder(sqlColsForSelect = sqlColsForSelect, tableNames = tableNames)
    with SearchQueryBuilder {

  private final val log = LoggerFactory.getLogger(getClass)
  private val searchParamTable = "search_parameter"
  private val searchDocumentTable = "search_document"

  def build(searchQuery: SearchQuery, countOnly: Boolean)
           (implicit ctx: Context, txId: TransactionId): SqlQuery = {
    // append tables that we are interested in based on features of this query
    val _tableNames: Set[String] = tableNames ++
      (if (searchQuery.isParametarized) Set(searchParamTable) else Set()) ++
      (if (searchQuery.isText) Set(searchDocumentTable) else Set())

    val (whereClause, sqlBindVals) = compileQuery(searchQuery, _tableNames)

    val sql = if (countOnly) {
      assembleQuery(
        select = "count(*) AS count",
        from = _tableNames.mkString(", "),
        where = whereClause)
    }
    else {
      assembleQuery(
        select = sqlColsForSelect,
        from = _tableNames.mkString(", "),
        where = whereClause,
        rpp = searchQuery.rpp,
        page = searchQuery.page)
    }

    log.debug(s"SQL QUERY: $sql with $sqlBindVals")
    SqlQuery(sql, sqlBindVals)
  }

  protected def compileQuery(searchQuery: SearchQuery, _tableNames: Set[String])
                            (implicit ctx: Context): (String, List[Any]) = {
    /**
      * Create DB query to create base search parameters, such as repo id and recycle flag.
      * We want to treat SearchParam's differently for parametarized search, as
      * these are no longer DB property lookups but search tokens.
      *
      * Note that we STRIP parameters for the base query - it will not create the SQL we need for
      * highly specialized search SQL
      */
    val baseSqlQuery = new Query(
      params = Map(C.Asset.IS_RECYCLED -> false),
      rpp = searchQuery.rpp,
      page = searchQuery.page)

    // bind value for possible text search
    val queryTextBindVal = if (searchQuery.isText) List(searchQuery.text.get) else List()
    // where clause for text search if any
    val queryTextWhereClause = if (searchQuery.isText) {
      List("body MATCH ?")
    } else {
      List()
    }
    val queryTextJoinClause = if (searchQuery.isText) {
      List(s"$searchDocumentTable.${C.SearchToken.ASSET_ID} = asset.id")
    } else {
      List()
    }

    val parametarizedSearchJoinClause = if (searchQuery.isParametarized) {
      List(s"$searchParamTable.${C.SearchToken.ASSET_ID} = asset.id")
    } else {
      List()
    }
    // base bind values
    val sqlBindVals = super[SqlQueryBuilder].getSqlBindVals(baseSqlQuery, _tableNames) :::
      queryTextBindVal

    // base where clauses
    val whereClauses = super[SqlQueryBuilder].getWhereClauses(baseSqlQuery, _tableNames) :::
      queryTextJoinClause :::
      parametarizedSearchJoinClause :::
      queryTextWhereClause

    compileSearchQuery(searchQuery, whereClauses, sqlBindVals)
  }
}

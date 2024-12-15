package software.altitude.core.dao.jdbc.querybuilder
import software.altitude.core.FieldConst
import software.altitude.core.RequestContext
import software.altitude.core.models.FieldType
import software.altitude.core.util.Query
import software.altitude.core.util.Query.QueryParam
import software.altitude.core.util.SearchQuery

object SearchQueryBuilder {
  private val ASSET_TABLE_NAME = "asset"
}

/** Common code for JDBC search query builders */
abstract class SearchQueryBuilder(selColumnNames: List[String]) extends SqlQueryBuilder[SearchQuery](selColumnNames, SearchQueryBuilder.ASSET_TABLE_NAME) {

  private val searchParamTable = "search_parameter"
  protected val searchDocumentTable = "search_document"

  private val notRecycledFilter: ClauseComponents =
    ClauseComponents(elements = List(s"${FieldConst.Asset.IS_RECYCLED} = ?"), bindVals = List(false))

  protected def textSearch(searchQuery: SearchQuery): ClauseComponents

  override protected def from(searchQuery: SearchQuery): ClauseComponents = {
    ClauseComponents(elements = allTableNames(searchQuery))
  }
  override protected def fromStr(clauseComponents: ClauseComponents): String = {
    val tableNames = clauseComponents.elements
    s"FROM ${tableNames.mkString(", ")}"
  }

  /** If we are joining a table - this will also include its name(s) */
  private def allTableNames(searchQuery: SearchQuery): List[String] = {
    val _tablesNames = List(SearchQueryBuilder.ASSET_TABLE_NAME) ++
      (if (searchQuery.isParameterized) Set(searchParamTable) else Set()) ++
      (if (searchQuery.isText) Set(searchDocumentTable) else Set())

    _tablesNames
  }

  override def buildSelectSql(query: SearchQuery): SqlQuery = {
    if (query.isSorted) {
      buildSelectSqlAsSubquery(query)
    } else {
      super.buildSelectSql(query)
    }
  }

  private def buildSelectSqlAsSubquery(query: SearchQuery): SqlQuery = {
    val allClauses = compileClauses(query)

    val subquerySql: String = s"""
        SELECT ${SearchQueryBuilder.ASSET_TABLE_NAME}.*
          ${fromStr(allClauses(SqlQueryBuilder.FROM))}
        ${whereStr(allClauses(SqlQueryBuilder.WHERE))}
      ${groupByStr(allClauses(SqlQueryBuilder.GROUP_BY))}
        ${havingStr(allClauses(SqlQueryBuilder.HAVING))}
      """

    val sql =
      selectStr(allClauses(SqlQueryBuilder.SELECT)) +
        s" FROM ($subquerySql) AS asset" +
        orderByStr(allClauses(SqlQueryBuilder.ORDER_BY)) +
        limitStr(query) +
        offsetStr(query)

    val bindValClauses = List(
      allClauses(SqlQueryBuilder.WHERE),
      allClauses(SqlQueryBuilder.ORDER_BY)
    )
    val bindVals = bindValClauses.foldLeft(List[Any]())((res, clause) => res ++ clause.bindVals)

    logger.debug(s"Select SQL: $sql with $bindVals")
    // println("SELECT", sql, bindVals)
    SqlQuery(sql, bindVals)
  }

  override protected def where(searchQuery: SearchQuery): ClauseComponents = {
    val repoIdElements = allTableNames(searchQuery).map(tableName => s"$tableName.${FieldConst.REPO_ID} = ?")
    val repoIdBindVals = allTableNames(searchQuery).map(_ => RequestContext.getRepository.persistedId)

    ClauseComponents(repoIdElements, repoIdBindVals) +
      textSearch(searchQuery) +
      notRecycledFilter +
      folderFilter(searchQuery) +
      fieldFilter(searchQuery) +
      searchDocumentJoin(searchQuery) +
      searchParameterJoin(searchQuery)
  }

  /** Generates a SQL "IN" clause for folder IDs */
  protected def folderFilter(searchQuery: SearchQuery): ClauseComponents = {
    if (searchQuery.folderIds.isEmpty) return ClauseComponents()

    // get ? placeholders equal to the number of folder ids
    val folderIdPlaceholders: String = List.fill(searchQuery.folderIds.size)("?").mkString(", ")

    ClauseComponents(
      elements = List(s"${FieldConst.Asset.FOLDER_ID} IN ($folderIdPlaceholders)"),
      bindVals = searchQuery.folderIds.toList
    )
  }

  protected def fieldFilter(searchQuery: SearchQuery): ClauseComponents = {
    val filters = searchQuery.params.map {
      el: (String, Any) =>
        val (_, value) = el
        value match {
          case _: String => "(field_id = ? AND field_value_kw = ?)"
          case _: Boolean => "(field_id = ? AND field_value_bool = ?)"
          case _: Number => "(field_id = ? AND field_value_num = ?)"
          case qParam: QueryParam =>
            qParam.paramType match {
              case Query.ParamType.EQ =>
                qParam.values.head match {
                  case _: String => "(field_id = ? AND field_value_kw = ?)"
                  case _: Boolean => "(field_id = ? AND field_value_bool = ?)"
                  case _: Number => "(field_id = ? AND field_value_num = ?)"
                }
              case _ => throw new IllegalArgumentException(s"This type of parameter is not supported: ${qParam.paramType}")
            }
          case _ => throw new IllegalArgumentException(s"This type of parameter is not supported: $value")
        }
    }.toList

    val bindVals = searchQuery.params.foldLeft(List[Any]()) {
      (res, el) =>
        val (metadataFieldId, value) = el

        // field id, value binds
        res :+ metadataFieldId :+ (value match {
          case qParam: QueryParam => qParam.values.head
          case _ => value
        })
    }

    if (filters.isEmpty) {
      ClauseComponents()
    } else {
      ClauseComponents(elements = List("(" + filters.mkString(" OR ") + ")"), bindVals = bindVals)
    }
  }

  protected def searchDocumentJoin(searchQuery: SearchQuery): ClauseComponents = {
    if (searchQuery.isText) {
      ClauseComponents(elements = List(s"$searchDocumentTable.asset_id = asset.id"))
    } else {
      ClauseComponents()
    }
  }

  protected def searchParameterJoin(searchQuery: SearchQuery): ClauseComponents = {
    if (searchQuery.isParameterized) {
      ClauseComponents(elements = List(s"$searchParamTable.asset_id = asset.id"))
    } else {
      ClauseComponents()
    }
  }

  override protected def groupBy(searchQuery: SearchQuery): ClauseComponents = {
    if (!searchQuery.isParameterized) return ClauseComponents()
    ClauseComponents(elements = List(s"asset.${FieldConst.ID}"))
  }

  override protected def having(searchQuery: SearchQuery): ClauseComponents = {
    if (!searchQuery.isParameterized) return ClauseComponents()
    ClauseComponents(elements = List(s"count(asset.${FieldConst.ID}) >= ${searchQuery.params.size}"))
  }

  override protected def orderBy(query: SearchQuery): ClauseComponents = {
    if (!query.isSorted) return ClauseComponents()

    val sort = query.searchSort.head
    val sortColumn = sort.field.fieldType match {
      case FieldType.NUMBER => "field_value_num"
      case FieldType.BOOL => "field_value_bool"
      case FieldType.KEYWORD => "field_value_kw"
      case FieldType.DATETIME => "field_value_dt"
      case _ => throw new IllegalArgumentException(s"This type of sort parameter is not supported: ${sort.field}")
    }

    val sql = ", search_parameter AS sort_param WHERE sort_param.repository_id = ? " +
      "AND sort_param.asset_id = asset.id " +
      "AND sort_param.field_id = ? " +
      s"ORDER BY sort_param.$sortColumn ${sort.direction}"

    ClauseComponents(List(sql), List(RequestContext.getRepository.persistedId, sort.field.persistedId))
  }

  override protected def orderByStr(clauseComponents: ClauseComponents): String = {
    if (clauseComponents.isEmpty) return ""
    clauseComponents.elements.mkString("")
  }

}

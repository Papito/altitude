package software.altitude.core.dao.jdbc.querybuilder

import software.altitude.core.util.Query.QueryParam
import software.altitude.core.util.{Query, SearchQuery}
import software.altitude.core.{Context, Const => C}


object SearchQueryBuilder {
  val ASSET_TABLE_NAME = "asset"
}

/**
  * Common code for JDBC search query builders
  */
abstract class SearchQueryBuilder(sqlColsForSelect: List[String])
  extends SqlQueryBuilder[SearchQuery](sqlColsForSelect, Set(SearchQueryBuilder.ASSET_TABLE_NAME)) {

  protected val searchParamTable = "search_parameter"
  protected val searchDocumentTable = "search_document"

  protected val notRecycledFilter = ClauseComponents(
    elements = List(s"${C.Asset.IS_RECYCLED} = ?"),
    bindVals = List(0))

  protected def textSearch(searchQuery: SearchQuery): ClauseComponents

  override protected def from(searchQuery: SearchQuery, ctx: Context): ClauseComponents = {
    ClauseComponents(elements = allTableNames(searchQuery))
  }

  /**
    * If we are joining a table - this will also include its name
    */
  private def allTableNames(searchQuery: SearchQuery): List[String] = {
    val _tablesNames = List(SearchQueryBuilder.ASSET_TABLE_NAME) ++
      (if (searchQuery.isParametarized) Set(searchParamTable) else Set()) ++
      (if (searchQuery.isText) Set(searchDocumentTable) else Set())

    _tablesNames
  }

  override def buildCountSql(query: SearchQuery)(implicit ctx: Context): SqlQuery = {
    // the COUNT clause is a more complicated subquery case when it's parameter search
    if (query.isParametarized) {
      // get the base SELECT query
      val selectSqlQuery = buildSelectSql(query)
      // make it a subquery of the main SELECT COUNT(*) query
      val countSql = s"SELECT COUNT(*) as count FROM (${selectSqlQuery.sqlAsString}) AS assets"
      SqlQuery(countSql, selectSqlQuery.bindValues)
    }
    else {
      super.buildCountSql(query)
    }
  }

  override protected def where(searchQuery: SearchQuery, ctx: Context): ClauseComponents = {
    val repoIdElements = allTableNames(searchQuery).map(tableName => s"$tableName.${C.Base.REPO_ID} = ?")
    val repoIdBindVals = allTableNames(searchQuery).map(_ => ctx.repo.id.get)

    ClauseComponents(repoIdElements, repoIdBindVals) +
      textSearch(searchQuery) +
      notRecycledFilter +
      folderFilter(searchQuery) +
      fieldFilter(searchQuery) +
      searchDocumentJoin(searchQuery) +
      searchParameterJoin(searchQuery)
  }

  /**
    * Generates a SQL "IN" clause for folder IDs
    */
  protected def folderFilter(searchQuery: SearchQuery): ClauseComponents = {
    if (searchQuery.folderIds.isEmpty) return ClauseComponents()

    // get ? placeholders equal to the number of folder ids
    val folderIdPlaceholders: String = List.fill(searchQuery.folderIds.size)("?").mkString(", ")

    ClauseComponents(
      elements = List(s"${C.Asset.FOLDER_ID} IN ($folderIdPlaceholders)"),
      bindVals = searchQuery.folderIds.toList
    )
  }

  protected def fieldFilter(searchQuery: SearchQuery): ClauseComponents = {
    val filters = searchQuery.params.map { el: (String, Any) =>
      val (_, value) = el
      value match {
        case _: String => "(field_id = ? AND field_value_kw = ?)"
        case _: Boolean => "(field_id = ? AND field_value_bool = ?)"
        case _: Number => "(field_id = ? AND field_value_num = ?)"
        case qParam: QueryParam => qParam.paramType match {
          case Query.ParamType.EQ => qParam.values.head match {
            case _: String => "(field_id = ? AND field_value_kw = ?)"
            case _: Boolean => "(field_id = ? AND field_value_bool = ?)"
            case _: Number => "(field_id = ? AND field_value_num = ?)"
          }
          case _ => throw new IllegalArgumentException(s"This type of parameter is not supported: ${qParam.paramType}")
        }
        case _ => throw new IllegalArgumentException(s"This type of parameter is not supported: $value")
      }
    }.toList

    val bindVals = searchQuery.params.foldLeft(List[Any]()) { (res, el) =>
      val (metadataFieldId, value) = el

      // field id, value binds
      res :+ metadataFieldId :+ (value match {
        case qParam: QueryParam => qParam.values.head
        case _ => value
      })
    }

    if (filters.isEmpty) {
      ClauseComponents()
    }
    else {
      ClauseComponents(
        elements = List("(" + filters.mkString(" OR ") + ")"), bindVals = bindVals)
    }
  }

  protected def searchDocumentJoin(searchQuery: SearchQuery): ClauseComponents = {
    if (searchQuery.isText) {
      ClauseComponents(elements = List(s"$searchDocumentTable.asset_id = asset.id"))
    }
    else {
      ClauseComponents()
    }
  }

  protected def searchParameterJoin(searchQuery: SearchQuery): ClauseComponents = {
    if (searchQuery.isParametarized) {
      ClauseComponents(elements = List(s"$searchParamTable.asset_id = asset.id"))
    }
    else {
      ClauseComponents()
    }
  }

  override protected def groupBy(searchQuery: SearchQuery, ctx: Context): ClauseComponents = {
    if (!searchQuery.isParametarized) return ClauseComponents()
    ClauseComponents(elements = List(s"asset.${C.Asset.ID}"))
  }

  override protected def having(searchQuery: SearchQuery, ctx: Context): ClauseComponents = {
    if (!searchQuery.isParametarized) return ClauseComponents()
    ClauseComponents(elements = List(s"count(asset.${C.Asset.ID}) >= ${searchQuery.params.size}"))
  }
}

package software.altitude.core.dao.jdbc.querybuilder

import org.slf4j.LoggerFactory
import software.altitude.core.util.Query.QueryParam
import software.altitude.core.util.{Query, SearchQuery}
import software.altitude.core.{Context, Const => C}

/**
  * Common code for JDBC search query builders
  */
abstract class SearchQueryBuilder(sqlColsForSelect: List[String], tableNames: Set[String])
  extends SqlQueryBuilder[SearchQuery](sqlColsForSelect, tableNames) {

  private final val log = LoggerFactory.getLogger(getClass)

  override protected def where(searchQuery: SearchQuery, ctx: Context): ClauseComponents = {
    super.where(searchQuery, ctx) + whereFolderFilter(searchQuery)
  }

  protected def whereFolderFilter(searchQuery: SearchQuery): ClauseComponents = {
    if (searchQuery.folderIds.isEmpty) return ClauseComponents()

    // get ? placeholders equal to the number of folder ids
    val folderIdPlaceholders: String = List.fill(searchQuery.folderIds.size)("?").mkString(", ")

    ClauseComponents(
      elements = List(s"${C.Asset.FOLDER_ID} IN ($folderIdPlaceholders)"),
      bindVals = searchQuery.folderIds.toList
    )
  }

}

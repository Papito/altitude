package software.altitude.core.dao.jdbc.querybuilder

import software.altitude.core.util.SearchQuery
import software.altitude.core.{Const => C}

/**
  * Common code for JDBC search query builders
  */
trait SearchQueryBuilder {
  def compileSearchQuery(searchQuery: SearchQuery,
                         whereClauses: List[String],
                         sqlBindVals: List[Any]): (String, List[Any]) = {

    // Narrow down the search based on folder IDs in the search query
    val folderIdPlaceholders: String = searchQuery.folderIds.toList.map {_ => "?"}.mkString(", ")
    val folderIdsWhereClause = if (searchQuery.folderIds.nonEmpty) {
      List(s"${C.Asset.FOLDER_ID} IN ($folderIdPlaceholders)")
    }
    else {
      List()
    }

    val _whereClauses = whereClauses ::: folderIdsWhereClause
    val _sqlBindVals = sqlBindVals ::: searchQuery.folderIds.toList
    val whereClause = s"""WHERE ${_whereClauses.mkString(" AND ")}"""

    (whereClause, _sqlBindVals)
  }
}

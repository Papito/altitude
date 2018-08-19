package software.altitude.core.dao.jdbc.querybuilder

import software.altitude.core.util.Query.QueryParam
import software.altitude.core.util.{Query, SearchQuery}
import software.altitude.core.{Context, Const => C}

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

    val _whereClauses = whereClauses ::: folderIdsWhereClause ::: getSearchParamWhereClauses(searchQuery)
    val _sqlBindVals = sqlBindVals ::: searchQuery.folderIds.toList ::: getSearchParamBindVals(searchQuery)
    val whereClause = s"""WHERE ${_whereClauses.mkString(" AND ")}""" +
      (if (searchQuery.isParametarized) s" GROUP BY asset.id HAVING count(asset.id) >= ${searchQuery.params.size}" else "")

    (whereClause, _sqlBindVals)
  }

  protected def getSearchParamWhereClauses(searchQuery: SearchQuery): List[String] = {

    val whereClauses = searchQuery.params.map { el: (String, Any) =>
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

    if (whereClauses.nonEmpty) List("(" + whereClauses.mkString(" OR ") + ")") else List()
  }

  protected def getSearchParamBindVals(searchQuery: SearchQuery): List[Any] = {
    searchQuery.params.foldLeft(List[Any]()) { (res, el) =>
      val (metadataFieldId, value) = el

      // field id, value binds
      res :+ metadataFieldId :+ (value match {
        case qParam: QueryParam => qParam.values.head
        case _ => value
      })
    }
  }
}

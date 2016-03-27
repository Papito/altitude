package altitude.dao.jdbc

import altitude.dao.QueryParser
import altitude.models.search.Query
import org.slf4j.LoggerFactory

class SqlQueryBuilder(sqlColsForSelect: String, tableName: String) extends QueryParser {
  private final val log = LoggerFactory.getLogger(getClass)

  def toSelectQuery(query: Query, countOnly: Boolean = false): SqlQuery = {
    val folderIds: Set[String] = getFolderIds(query)

    // filter out system parameters
    val params = getParams(query)

    val (sqlColumns, sqlValues) = params.unzip
    // create pairs of column names and value placeholders, to be joined in the final clause
    val whereClauses: List[String] = sqlColumns.map(_ + " = ?").toList

    val whereClause = whereClauses.length match {
      case 0 => ""
      case _ => s"""WHERE ${whereClauses.mkString(" AND ")}"""
    }

    val folderClause = folderIds.isEmpty match {
      case true => ""
      case false => whereClause.isEmpty match {
        case false => " AND folder_id in (" + folderIds.map(x => "?").mkString(",") + ")"
        case true => "WHERE folder_id in (" + folderIds.toSeq.map(x => "?").mkString(",") + ")"
      }
    }

    val whereSegment = s"$whereClause $folderClause"

    val sql = countOnly match {
      case false => assembleQuery(
        select = sqlColsForSelect,
        from = tableName,
        where = whereSegment,
        rpp = query.rpp,
        page = query.page)
      case true => assembleQuery(
        select = "count(*) AS count",
        from = tableName,
        where = whereSegment,
        rpp = 0)
    }

    val bindValues = sqlValues.toList ::: folderIds.toList
    log.debug(s"SQL QUERY: $sql with $bindValues")
    SqlQuery(sql, bindValues)
  }

  protected def assembleQuery(select: String, from: String, where: String, rpp: Int = 0, page: Int = 0): String = {
    val sqlWithoutPaging = s"""
      SELECT $select
        FROM $from
        $where
    """

    rpp match  {
      case 0 => sqlWithoutPaging
      case _ =>  sqlWithoutPaging + s"""
        LIMIT $rpp
        OFFSET ${(page - 1) * rpp}"""
    }
  }
}

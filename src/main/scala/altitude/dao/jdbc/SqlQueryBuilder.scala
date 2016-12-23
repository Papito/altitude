package altitude.dao.jdbc

import altitude.dao.QueryParser
import altitude.models.search.Query
import altitude.{Const => C, Context}
import org.slf4j.LoggerFactory

class SqlQueryBuilder(sqlColsForSelect: String, tableName: String) extends QueryParser {
  private final val log = LoggerFactory.getLogger(getClass)

  def toSelectQuery(query: Query, countOnly: Boolean = false)(implicit ctx: Context): SqlQuery = {
    val folderIds: Set[String] = getFolderIds(query)

    // get the query params - and add the repository
    val params = getParams(query) + (C.Base.REPO_ID -> ctx.repo.id.get)

    // filter out system parameters
    val (sqlColumns, sqlValues) = params.unzip
    // create pairs of column names and value placeholders, to be joined in the final clause
    val whereClauses: List[String] = sqlColumns.map(_ + " = ?").toList

    val whereClause = whereClauses.length match {
      case 0 => ""
      case _ => s"""WHERE ${whereClauses.mkString(" AND ")}"""
    }

    val folderClause = folderIds.nonEmpty match {
      case true => whereClause.isEmpty match {
        case false => s" AND ${C.Asset.FOLDER_ID} in (" + folderIds.toSeq.map(x => "?").mkString(",") + ")"
        case true => s"WHERE ${C.Asset.FOLDER_ID} in (" + folderIds.toSeq.map(x => "?").mkString(",") + ")"
      }
      case false => ""
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

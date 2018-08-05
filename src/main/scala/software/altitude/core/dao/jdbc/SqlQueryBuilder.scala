package software.altitude.core.dao.jdbc

import org.slf4j.LoggerFactory
import software.altitude.core.dao.QueryParser
import software.altitude.core.transactions.TransactionId
import software.altitude.core.util.Query
import software.altitude.core.{Context, Const => C}

/**
 * An advanced ORM replacement :)
 *
 * @param sqlColsForSelect columns to select
 * @param tableName table name to query
 */
class SqlQueryBuilder(sqlColsForSelect: String, tableName: String) extends QueryParser {
  private final val log = LoggerFactory.getLogger(getClass)

  def toSelectQuery(query: Query, countOnly: Boolean = false)(implicit ctx: Context, txId: TransactionId): SqlQuery = {
    val folderIds: Set[String] = getFolderIds(query)

    // get the query params - and add the repository
    val params = getParams(query) + (C.Base.REPO_ID -> ctx.repo.id.get)

    val (sqlColumns, sqlValues) = params.unzip
    // create pairs of column names and value placeholders, to be joined in the final clause
    val whereClauses: List[String] = sqlColumns.map(_ + " = ?").toList

    val whereClause = whereClauses.length match {
      case 0 => ""
      case _ => s"""WHERE ${whereClauses.mkString(" AND ")}"""
    }

    val folderClause = if (folderIds.nonEmpty) {
      if (whereClause.isEmpty) {
        s"WHERE ${C.Asset.FOLDER_ID} in (" + folderIds.toSeq.map(x => "?").mkString(",") + ")"
      }
      else {
        s" AND ${C.Asset.FOLDER_ID} in (" + folderIds.toSeq.map(x => "?").mkString(",") + ")"
      }
    }
    else {
      ""
    }

    val whereSegment = s"$whereClause $folderClause"

    val sql = if (countOnly) {
      assembleQuery(
        select = "count(*) AS count",
        from = tableName,
        where = whereSegment)
    }
    else {
      assembleQuery(
        select = sqlColsForSelect,
        from = tableName,
        where = whereSegment,
        rpp = query.rpp,
        page = query.page)
    }

    val bindValues = sqlValues.toList ::: folderIds.toList
    log.debug(s"SQL QUERY: $sql with $bindValues")
    SqlQuery(sql, bindValues)
  }

  protected def assembleQuery(select: String, from: String, where: String, rpp: Int = 0, page: Int = 0): String = {
    val sqlWithoutPaging = s"SELECT $select FROM $from $where"

    rpp match {
      case 0 => sqlWithoutPaging
      case _ => sqlWithoutPaging + s"""
        LIMIT $rpp
        OFFSET ${(page - 1) * rpp}"""
    }
  }
}

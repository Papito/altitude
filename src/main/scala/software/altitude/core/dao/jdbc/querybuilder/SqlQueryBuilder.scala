package software.altitude.core.dao.jdbc.querybuilder

import org.slf4j.LoggerFactory
import software.altitude.core.transactions.TransactionId
import software.altitude.core.util.Query
import software.altitude.core.util.Query.QueryParam
import software.altitude.core.{Context, Const => C}

/**
 * An advanced ORM replacement :)
 *
 * @param sqlColsForSelect columns to select
 * @param tableName table name to query
 */
class SqlQueryBuilder(sqlColsForSelect: String, tableName: String) {
  private final val log = LoggerFactory.getLogger(getClass)

  def toSelectQuery(query: Query, countOnly: Boolean = false)
                   (implicit ctx: Context, txId: TransactionId): SqlQuery = {

    val (sqlColumns, sqlValues) = getParams(query).unzip

    val whereClause = getWhereClause(sqlColumns.toSeq)

    val sql = if (countOnly) {
      assembleQuery(
        select = "count(*) AS count",
        from = tableName,
        where = whereClause)
    }
    else {
      assembleQuery(
        select = sqlColsForSelect,
        from = tableName,
        where = whereClause,
        rpp = query.rpp,
        page = query.page)
    }

    log.debug(s"SQL QUERY: $sql with $sqlValues")
    println(sql, sqlValues)
    SqlQuery(sql, sqlValues.toList)
  }

  /**
    * Massage the parameters and make them SQL-read (no booleans, for example).
    * THEN, append the repo ID
    * @param query
    * @return Map of String->Any values, that are ready to be bound to a SQL select statement
    */
  protected def getParams(query: Query)(implicit ctx: Context): Map[String, Any] = {
    query.params.map { v: (String, Any) =>
      v._2 match {
        case scalar: String => (v._1, scalar)
        case scalar: Boolean => if (scalar) (v._1, 1) else (v._1, 0)
        case qPara6m: QueryParam => qParam.paramType match {
          case Query.ParamType.EQ => {
            (v._1, qParam.values.head)
          }
          case Query.ParamType.IN => {
            println("!!!!!!!!!!")
            (v._1, qParam.values.head)
          }
          case _ => throw new IllegalArgumentException(
            s"This type of query parameter is not supported: ${qParam.paramType}")
        }
        case _ => throw new IllegalArgumentException("This type of parameter is not supported")
      }
    } + (C.Base.REPO_ID -> ctx.repo.id.get)
  }

  protected def getWhereClause(sqlColumns: Seq[String])(implicit ctx: Context): String = {
    // create pairs of column names and value placeholders, to be joined in the final clause
    val whereClauses: List[String] = sqlColumns.toList.map(_ + " = ?")

    whereClauses.length match {
      case 0 => ""
      case _ => s"""WHERE ${whereClauses.mkString(" AND ")}"""
    }
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

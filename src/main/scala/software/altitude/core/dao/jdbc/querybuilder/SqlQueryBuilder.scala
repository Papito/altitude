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

    val (whereClause, sqlBindVals) = compileQuery(query)

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

    log.debug(s"SQL QUERY: $sql with $sqlBindVals")
    SqlQuery(sql, sqlBindVals)
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

  protected def compileQuery(query: Query)(implicit ctx: Context): (String, List[Any]) = {

    val sqlBindVals: List[Any] = query.params.foldLeft(List[Any]()) { (res, el: (String, Any)) =>
      val value = el._2
      value match {
        // string or integer values as is
        case _: String => res :+ value
        case _: Number => res :+ value
        // boolean becomes 0 or 1
        case _: Boolean => res :+ (if (value == true) 1 else 0)
        // extract all values from qParam
        case qParam: QueryParam => res ::: qParam.values.toList
        case _ => throw new IllegalArgumentException(s"This type of parameter is not supported: $value")
      }
    } :+ ctx.repo.id.get

    val whereClauses: List[String] = query.params.map { el: (String, Any) =>
      val (columnName, value) = el
      value match {
        case _: String => s"$columnName = ?"
        case _: Boolean => s"$columnName = ?"
        case _: Number => s"$columnName = ?"
        case qParam: QueryParam => qParam.paramType match {
          case Query.ParamType.IN => {
            val placeholders: String = qParam.values.toList.map {_ => "?"}.mkString(", ")
            s"$columnName IN ($placeholders)"
          }
          case Query.ParamType.EQ => s"$columnName = ?"

          case _ => throw new IllegalArgumentException(s"This type of parameter is not supported: ${qParam.paramType}")
        }
        case _ => throw new IllegalArgumentException(s"This type of parameter is not supported: $value")
      }
    }.toList :+ s"${C.Base.REPO_ID} = ?"

    val whereClause = s"""WHERE ${whereClauses.mkString(" AND ")}"""

    (whereClause, sqlBindVals)
  }
}

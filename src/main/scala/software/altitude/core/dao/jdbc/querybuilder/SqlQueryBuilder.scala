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
  * @param tableNames table names for select
  */
class SqlQueryBuilder(sqlColsForSelect: String, tableNames: Set[String]) {
  private final val log = LoggerFactory.getLogger(getClass)


  // convenience constructor for the common case of just one table
  def this(sqlColsForSelect: String, tableName: String) = {
    this(sqlColsForSelect, Set(tableName))
  }

  def build(query: Query, countOnly: Boolean = false)
           (implicit ctx: Context, txId: TransactionId): SqlQuery = {
    val (whereClause, sqlBindVals) = compileQuery(query, tableNames)

    val sql = if (countOnly) {
      assembleQuery(
        select = "count(*) AS count",
        from = tableNames.mkString(", "),
        where = whereClause)
    }
    else {
      assembleQuery(
        select = sqlColsForSelect,
        from = tableNames.mkString(", "),
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

  protected def compileQuery(query: Query, _tableNames: Set[String])(implicit ctx: Context): (String, List[Any]) = {
    val sqlBindVals = getSqlBindVals(query, _tableNames)
    val whereClauses = getWhereClauses(query, _tableNames)
    val whereClause = s"""WHERE ${whereClauses.mkString(" AND ")}"""
    (whereClause, sqlBindVals)
  }

  protected def getSqlBindVals(query: Query, _tableNames: Set[String])(implicit ctx: Context): List[Any] = {
    query.params.foldLeft(List[Any]()) { (res, el: (String, Any)) =>
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
    } ::: _tableNames.toSeq.map(_ => ctx.repo.id.get).toList // repo id for each table
  }

  protected def getWhereClauses(query: Query, _tableNames: Set[String])(implicit ctx: Context): List[String] = {
    query.params.map { el: (String, Any) =>
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
    }.toList ::: _tableNames.map(tableName => s"$tableName.${C.Base.REPO_ID} = ?").toList
  }
}

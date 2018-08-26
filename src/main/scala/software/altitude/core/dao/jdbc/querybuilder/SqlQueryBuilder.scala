package software.altitude.core.dao.jdbc.querybuilder

import org.slf4j.LoggerFactory
import software.altitude.core.transactions.TransactionId
import software.altitude.core.util.Query
import software.altitude.core.util.Query.QueryParam
import software.altitude.core.{Context, Const => C}

protected object SqlQueryBuilder {
  val SELECT = "select"
  val FROM = "from"
  val JOIN = "join"
  val WHERE = "where"
  val GROUP_BY = "group_by"
  val ORDER_BY = "order_by"
}

class SqlQueryBuilder(sqlColsForSelect: List[String], tableNames: Set[String]) {
  private final val log = LoggerFactory.getLogger(getClass)

  protected case class ClauseComponents(elements: List[String], bindVals: List[Any] = List())
  type ClauseGeneratorType = (Query, Context) => ClauseComponents

  // convenience constructor for the common case of just one table
  def this(sqlColsForSelect: List[String], tableName: String) = {
    this(sqlColsForSelect, Set(tableName))
  }

  def build(query: Query,
            sqlColsForSelect: List[String] = sqlColsForSelect,
            tableNames: Set[String] = tableNames,
            countOnly: Boolean = false)
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
        select = sqlColsForSelect.mkString(", "),
        from = tableNames.mkString(", "),
        where = whereClause,
        rpp = query.rpp,
        page = query.page)
    }

    log.debug(s"SQL QUERY: $sql with $sqlBindVals")
    SqlQuery(sql, sqlBindVals)
  }

  def build2(query: Query)(implicit ctx: Context): SqlQuery = {
    // a list of methods we will apply to build final query components
    val chainMethods: List[(String, ClauseGeneratorType)] = List(
      (SqlQueryBuilder.SELECT, this.select),
      (SqlQueryBuilder.FROM, this.from),
      (SqlQueryBuilder.WHERE, this.where)
    )

    // collect component statements so we can refer to them when we build the SQL string
    val allClauseComponents = chainMethods.foldLeft(Map[String, ClauseComponents]()) { (res, m) =>
      val clauseName = m._1
      val clauseGeneratorMethod = m._2
      val clauseComponents = clauseGeneratorMethod(query, ctx)
      res + (clauseName -> clauseComponents)
    }

    val sql: String  = selectStr(allClauseComponents) +
      fromStr(allClauseComponents) +
      whereStr(allClauseComponents) +
      limitStr(query) +
      offsetStr(query)

    val bindVals = allClauseComponents.foldLeft(List[Any]()) { (res, comp) =>
      res ++ comp._2.bindVals
    }

    SqlQuery(sql, bindVals)
  }

  protected def select(query: Query, ctx: Context) = ClauseComponents(elements = sqlColsForSelect)
  protected def selectStr(data: Map[String, ClauseComponents]): String = {
    val columnNames = data(SqlQueryBuilder.SELECT).elements
    s"SELECT ${columnNames.mkString(", ")}"
  }

  protected def from(query: Query, ctx: Context) = ClauseComponents(elements = tableNames.toList)
  protected def fromStr(data: Map[String, ClauseComponents]): String = {
    val tableNames = data(SqlQueryBuilder.FROM).elements
    s" FROM ${tableNames.mkString(", ")}"
  }

  protected def where(query: Query, ctx: Context): ClauseComponents = {
    val elements = query.params.map { el: (String, Any) =>
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
    }.toList ::: tableNames.map(tableName => s"$tableName.${C.Base.REPO_ID} = ?").toList

    val bindVals = query.params.foldLeft(List[Any]()) { (res, el: (String, Any)) =>
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
    } ::: tableNames.toSeq.map(_ => ctx.repo.id.get).toList // repo id for each table

    ClauseComponents(elements, bindVals)
  }

  protected def whereStr(data: Map[String, ClauseComponents]): String = {
    val whereClauses = data(SqlQueryBuilder.WHERE).elements
    s" WHERE ${whereClauses.mkString(" AND ")}"
  }

  protected def limitStr(query: Query): String = if (query.rpp > 0) s" LIMIT ${query.rpp}" else ""
  protected def offsetStr(query: Query): String = if (query.rpp > 0) s" OFFSET ${(query.page - 1) * query.rpp}" else ""


  // ------------------------------------------------------------------------------------------------------------------

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

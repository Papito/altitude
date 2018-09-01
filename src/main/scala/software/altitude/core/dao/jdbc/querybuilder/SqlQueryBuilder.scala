package software.altitude.core.dao.jdbc.querybuilder

import org.slf4j.LoggerFactory
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

class SqlQueryBuilder[QueryT <: Query](sqlColsForSelect: List[String], tableNames: Set[String]) {
  private final val log = LoggerFactory.getLogger(getClass)

  protected case class ClauseComponents(elements: List[String] = List(), bindVals: List[Any] = List()) {
    // use the + operator to smuch two clause components together
    def +(that: ClauseComponents): ClauseComponents =
      ClauseComponents(this.elements ::: that.elements, this.bindVals ::: that.bindVals)
  }

  type ClauseGeneratorType = (QueryT , Context) => ClauseComponents

  protected val chainMethods: List[(String, ClauseGeneratorType)] = List(
    (SqlQueryBuilder.SELECT, this.select),
    (SqlQueryBuilder.FROM, this.from),
    (SqlQueryBuilder.WHERE, this.where)
  )

  // convenience constructor for the common case of just one table
  def this(sqlColsForSelect: List[String], tableName: String) = {
    this(sqlColsForSelect, Set(tableName))
  }

  def buildSelectSql(query: QueryT)(implicit ctx: Context): SqlQuery = {
    val allClauses = compileClauses(query, ctx)

    val sql: String  = selectStr(allClauses) +
      fromStr(allClauses) +
      whereStr(allClauses) +
      limitStr(query) +
      offsetStr(query)

    val bindVals = allClauses.foldLeft(List[Any]()) { (res, comp) =>
      res ++ comp._2.bindVals
    }

    log.debug(s"Select SQL: $sql with $bindVals")
    println(sql, bindVals)
    SqlQuery(sql, bindVals)
  }

  def buildCountSql(query: QueryT)(implicit ctx: Context): SqlQuery = {
    // the SQL is the same but the WHERE clause is just the COUNT
    val selectClauseForCount = ClauseComponents(List("COUNT(*) AS count"))
    val allClauses = compileClauses(query, ctx) + (
      SqlQueryBuilder.SELECT -> selectClauseForCount)

    val sql: String  = selectStr(allClauses) + fromStr(allClauses) + whereStr(allClauses)

    val bindVals = allClauses.foldLeft(List[Any]()) { (res, comp) =>
      res ++ comp._2.bindVals
    }

    log.debug(s"Count SQL: $sql with $bindVals")
    SqlQuery(sql, bindVals)
  }

  protected def compileClauses(query: QueryT, ctx: Context): Map[String, ClauseComponents] = {
    // collect clauses so we can refer to them when we build the SQL string
    chainMethods.foldLeft(Map[String, ClauseComponents]()) { (res, m) =>
      val clauseName = m._1
      val clauseGeneratorMethod = m._2
      val clauseComponents = clauseGeneratorMethod(query, ctx)
      res + (clauseName -> clauseComponents)
    }
  }

  protected def select(query: QueryT, ctx: Context) = ClauseComponents(elements = sqlColsForSelect)
  protected def selectStr(data: Map[String, ClauseComponents]): String = {
    val columnNames = data(SqlQueryBuilder.SELECT).elements
    s"SELECT ${columnNames.mkString(", ")}"
  }

  protected def from(query: QueryT, ctx: Context) = ClauseComponents(elements = tableNames.toList)
  protected def fromStr(data: Map[String, ClauseComponents]): String = {
    val tableNames = data(SqlQueryBuilder.FROM).elements
    s" FROM ${tableNames.mkString(", ")}"
  }

  protected def where(query: QueryT, ctx: Context): ClauseComponents = {
    // FIXME: find a better way to get elements and bindvals in one swoop
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

  protected def limitStr(query: QueryT): String = if (query.rpp > 0) s" LIMIT ${query.rpp}" else ""
  protected def offsetStr(query: QueryT): String = if (query.rpp > 0) s" OFFSET ${(query.page - 1) * query.rpp}" else ""
}

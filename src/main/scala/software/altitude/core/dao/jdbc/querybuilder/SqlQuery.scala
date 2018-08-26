package software.altitude.core.dao.jdbc.querybuilder

import software.altitude.core.util.Query

case class SqlQuery(sqlAsString: String, bindValues: List[Any])

object _SqlQuery {
  val SELECT = "select"
  val FROM = "from"
  val JOIN = "join"
  val WHERE = "where"
  val GROUP_BY = "group_by"
  val ORDER_BY = "order_by"
  val LIMIT = "limit"
  val OFFSET = "offset"
}

case class _SqlQuery private (query: Query,
                              private val data: Map[String, List[String]],
                              private val bindVals: List[Any]) {

  def this(query: Query) = {
    this(query, Map(), List())
  }
  /*
  private val select: List[String] = List()
  private val from: List[String] = List()
  private val join: List[String] = List()
  private val where: List[String] = List()
  private val group_by: List[String] = List()
  private val order_by: List[String] = List()
  private val limit: Option[Int] = None
  private val offset: Option[Int] = None
  private val bindValues: List[Any] = List()
*/

  override def toString: String = ""
}
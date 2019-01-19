package software.altitude.core.util

import play.api.libs.json.JsObject

object QueryResult {
  val EMPTY = QueryResult(List[JsObject](), total = 1, rpp = 0, sort = None)
}

case class QueryResult(records: List[JsObject], total: Int, rpp: Int, sort: Option[Sort]) {
  val nonEmpty: Boolean = records.nonEmpty
  val isEmpty: Boolean = records.isEmpty
  val totalPages: Int = Math.ceil(total / rpp.toDouble).toInt
}

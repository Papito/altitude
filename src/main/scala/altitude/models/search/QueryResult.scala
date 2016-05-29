package altitude.models.search

import play.api.libs.json.JsObject

object QueryResult {
  val EMPTY = QueryResult(List[JsObject](), 1, Query())
}

case class QueryResult(records: List[JsObject], total: Int, query: Query) {
  val nonEmpty = records.nonEmpty
  val totalPages: Int = Math.ceil(total / query.rpp.toDouble).toInt
}

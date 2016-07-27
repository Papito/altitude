package altitude.models.search

import play.api.libs.json.JsObject

object QueryResult {
  val EMPTY = QueryResult(List[JsObject](), 1, None)
}

case class QueryResult(records: List[JsObject], total: Int, query: Option[Query]) {
  val nonEmpty = records.nonEmpty
  val totalPages: Int = if (query.isDefined) Math.ceil(total / query.get.rpp.toDouble).toInt else 0
}

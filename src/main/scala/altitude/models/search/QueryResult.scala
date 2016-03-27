package altitude.models.search

import play.api.libs.json.JsObject

case class QueryResult(records: List[JsObject], total: Int, query: Query) {
  val nonEmpty = records.nonEmpty
  val totalPages: Int = Math.ceil(total / query.rpp.toDouble).toInt
}

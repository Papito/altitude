package software.altitude.core.util

import play.api.libs.json.JsObject

case class SearchResult(records: List[JsObject], total: Int, rpp: Int, sort: Option[SearchSort]) {
  val nonEmpty: Boolean = records.nonEmpty
  val isEmpty: Boolean = records.isEmpty
  val totalPages: Int = Math.ceil(total / rpp.toDouble).toInt
}

package software.altitude.core.util

import play.api.libs.json.{JsObject, Json}

case class SearchResult(records: List[JsObject], total: Int, rpp: Int, sort: List[SearchSort]) {
  val nonEmpty: Boolean = records.nonEmpty
  val isEmpty: Boolean = records.isEmpty
  val totalPages: Int = Math.ceil(total / rpp.toDouble).toInt
}

package software.altitude.core.util

import play.api.libs.json.JsObject

object QueryResult {
  val EMPTY = QueryResult(List[JsObject](), 1, None)
}

case class QueryResult(records: List[JsObject], total: Int, query: Option[Query]) {
  val nonEmpty = records.nonEmpty
  val isEmpty = records.isEmpty

  val totalPages: Int = query match {
    case Some(q) if isEmpty => 0
    case None if isEmpty => 0
    case None if nonEmpty => 1
    case Some(q) if q.rpp == 0 && nonEmpty => 1
    case Some(q) if q.rpp > 0 => Math.ceil(total / query.get.rpp.toDouble).toInt
    case _ => throw new IllegalStateException
  }
}

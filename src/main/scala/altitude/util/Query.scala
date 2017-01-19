package altitude.util

case class Query(params: Map[String, Object] = Map(), rpp: Int = 0, page: Int = 1) {
  if (rpp < 0) throw new IllegalArgumentException(s"Invalid results per page value: $rpp")
  if (page < 1) throw new IllegalArgumentException(s"Invalid page value: $page")
}

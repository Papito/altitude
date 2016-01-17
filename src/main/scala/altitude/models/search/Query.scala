package altitude.models.search

case class Query(params: Map[String, Object] = Map(), rpp: Int = 0, page: Int = 1)

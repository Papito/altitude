package altitude.models.search

case class Query(params: Map[String, String] = Map(), rpp: Int = 1, page: Int = 1)

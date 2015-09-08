package altitude.models.search

case class Query(params: Map[String, String] = Map(), rpp: Int = 50, page: Int = 1)

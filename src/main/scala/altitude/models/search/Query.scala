package altitude.models.search

case class Query(params: Map[String, String] = Map(), rpp: Int = 20, page: Int = 1)

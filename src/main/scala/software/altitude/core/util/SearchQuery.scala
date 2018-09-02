package software.altitude.core.util

class SearchQuery(val text: Option[String] = None,
                  params: Map[String, Any] = Map(),
                  val folderIds: Set[String] = Set(),
                  rpp: Int = 0,
                  page: Int = 1)
  extends Query(params = params, rpp = rpp, page = page) {

  val isParametarized: Boolean = params.nonEmpty
  val isText: Boolean = text.nonEmpty

  override def add(_params: (String, Any)*): SearchQuery = new SearchQuery(
    text = text,
    folderIds = folderIds,
    params = params ++ _params,
    rpp = rpp,
    page = page)
}

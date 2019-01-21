package software.altitude.core.util

import software.altitude.core.models.MetadataField

case class SearchSort(field: MetadataField, direction: SortDirection.Value)

class SearchQuery(val text: Option[String] = None,
                  params: Map[String, Any] = Map(),
                  val folderIds: Set[String] = Set(),
                  rpp: Int = 0,
                  page: Int = 1,
                  val searchSort: Option[SearchSort] = None)
  extends Query(params = params, rpp = rpp, page = page) {

  if (sort.isDefined) {
    throw new IllegalArgumentException("Cannot use 'sort' in this context - use 'searchSort'")
  }

  val isParameterized: Boolean = params.nonEmpty
  val isText: Boolean = text.nonEmpty
  val isSorted: Boolean = searchSort.isDefined

  override def add(_params: (String, Any)*): SearchQuery = new SearchQuery(
    text = text,
    folderIds = folderIds,
    params = params ++ _params,
    rpp = rpp,
    page = page,
    searchSort = searchSort)
}

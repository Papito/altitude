package software.altitude.core.util

import software.altitude.core.models.MetadataField

case class SearchSort(field: MetadataField, direction: SortDirection.Value)

class SearchQuery(val text: Option[String] = None,
                  params: Map[String, Any] = Map(),
                  val folderIds: Set[String] = Set(),
                  rpp: Int = 0,
                  page: Int = 1,
                  val searchSort: List[SearchSort] = List())
  extends Query(params = params, rpp = rpp, page = page) {

  if (sort.nonEmpty) {
    throw new IllegalArgumentException("Cannot use 'sort' in this context - use 'searchSort'")
  }

  if (searchSort.size > 1) {
    throw new IllegalArgumentException("Only one sort currently supported'")
  }

  val isParameterized: Boolean = params.nonEmpty
  val isText: Boolean = text.nonEmpty
  override val isSorted: Boolean = searchSort.nonEmpty

  override def add(_params: (String, Any)*): SearchQuery = new SearchQuery(
    text = text,
    folderIds = folderIds,
    params = params ++ _params,
    rpp = rpp,
    page = page,
    searchSort = searchSort)
}

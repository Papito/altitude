package software.altitude.core.util

import software.altitude.core.models.MetadataField

case class SearchSort(field: MetadataField, direction: SortDirection.Value)

class SearchQuery(val text: Option[String] = None,
                  params: Map[String, Any] = Map(),
                  val folderIds: Set[String] = Set(),
                  rpp: Int = 0,
                  page: Int = 1,
                  sort: Option[SearchSort] = None)
  extends Query(params = params, rpp = rpp, page = page, sort = sort) {

  val isParameterized: Boolean = params.nonEmpty
  val isText: Boolean = text.nonEmpty
  val isSorted: Boolean = sort.isDefined

  override def add(_params: (String, Any)*): SearchQuery = new SearchQuery(
    text = text,
    folderIds = folderIds,
    params = params ++ _params,
    rpp = rpp,
    page = page,
    sort = sort)
}

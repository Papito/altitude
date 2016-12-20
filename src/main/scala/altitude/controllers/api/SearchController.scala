package altitude.controllers.api

import altitude.models.search.Query
import altitude.{Const => C, Context}
import org.scalatra.Ok
import org.slf4j.LoggerFactory
import play.api.libs.json.Json

class SearchController extends BaseApiController {
  private final val log = LoggerFactory.getLogger(getClass)

  get("/") {
    val foldersQuery = params.getOrElse(C.Api.Search.FOLDERS, "")

    val q = Query(
      user.get,
      params = Map(C.Api.Folder.QUERY_ARG_NAME -> foldersQuery),
      rpp = 20, page = 1)

    val results = app.service.library.search(q)

    Ok(Json.obj(
      C.Api.Search.ASSETS -> results.records,
      C.Api.TOTAL_RECORDS -> results.total,
      C.Api.CURRENT_PAGE -> q.page,
      C.Api.TOTAL_PAGES -> results.totalPages,
      C.Api.RESULTS_PER_PAGE -> q.rpp
    ))
  }

  get(s"/p/:${C.Api.Search.PAGE}/rpp/:${C.Api.Search.RESULTS_PER_PAGE}") {
    // FIXME: magic constants
    val rpp = params.getOrElse(C.Api.Search.RESULTS_PER_PAGE, "20").toInt
    // FIXME: magic constants
    val page = params.getOrElse(C.Api.Search.PAGE, "1").toInt
    val foldersQuery = this.params.getOrElse(C.Api.Search.FOLDERS, "")

    val q = Query(
      user.get,
      params = Map(C.Api.Folder.QUERY_ARG_NAME -> foldersQuery),
      rpp = rpp, page = page)

    val results = app.service.library.search(q)

    Ok(Json.obj(
      C.Api.Search.ASSETS -> results.records,
      C.Api.TOTAL_RECORDS -> results.total,
      C.Api.CURRENT_PAGE -> q.page,
      C.Api.TOTAL_PAGES -> results.totalPages,
      C.Api.RESULTS_PER_PAGE -> q.rpp
    ))
  }
}

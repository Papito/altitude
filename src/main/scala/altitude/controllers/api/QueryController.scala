package altitude.controllers.api

import altitude.util.Query
import altitude.{Const => C}
import org.scalatra.Ok
import org.slf4j.LoggerFactory
import play.api.libs.json.Json

class QueryController extends BaseApiController {
  private final val log = LoggerFactory.getLogger(getClass)

  get("/") {
    val foldersQuery = params.getOrElse(C.Api.Search.FOLDERS, "")

    val q = Query(
      params = Map(C.Api.Folder.QUERY_ARG_NAME -> foldersQuery),
      rpp = 20, page = 1)

    val results = app.service.library.query(q)

    Ok(Json.obj(
      C.Api.Search.ASSETS -> results.records,
      C.Api.TOTAL_RECORDS -> results.total,
      C.Api.CURRENT_PAGE -> q.page,
      C.Api.TOTAL_PAGES -> results.totalPages,
      C.Api.RESULTS_PER_PAGE -> q.rpp
    ))
  }

  get(s"/p/:${C.Api.Search.PAGE}/rpp/:${C.Api.Search.RESULTS_PER_PAGE}") {
    val rpp = params.getOrElse(C.Api.Search.RESULTS_PER_PAGE, C.DEFAULT_RPP).toInt
    val page = params.getOrElse(C.Api.Search.PAGE, "1").toInt
    val foldersQuery = this.params.getOrElse(C.Api.Search.FOLDERS, "")

    val q = Query(
      params = Map(C.Api.Folder.QUERY_ARG_NAME -> foldersQuery),
      rpp = rpp, page = page)

    val results = app.service.library.query(q)

    Ok(Json.obj(
      C.Api.Search.ASSETS -> results.records,
      C.Api.TOTAL_RECORDS -> results.total,
      C.Api.CURRENT_PAGE -> q.page,
      C.Api.TOTAL_PAGES -> results.totalPages,
      C.Api.RESULTS_PER_PAGE -> q.rpp
    ))
  }
}
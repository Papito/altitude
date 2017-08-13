package altitude.controllers.api

import altitude.controllers.Util
import altitude.models.Asset
import altitude.util.Query
import altitude.{Const => C}
import org.scalatra.{ActionResult, Ok}
import org.slf4j.LoggerFactory
import play.api.libs.json.Json

class QueryController extends BaseApiController {
  private final val log = LoggerFactory.getLogger(getClass)

  get("/") {
    val foldersQuery = params.getOrElse(C.Api.Search.FOLDERS, "")

    val folderId = foldersQuery.isEmpty match {
      case true => app.REPO.rootFolderId
      case false => foldersQuery
    }

    defaultQuery(folderId)
  }

  get("/triage") {
    defaultQuery(app.REPO.triageFolderId)
  }

  get(s"/p/:${C.Api.Search.PAGE}/rpp/:${C.Api.Search.RESULTS_PER_PAGE}") {
    val rpp = params.getOrElse(C.Api.Search.RESULTS_PER_PAGE, C.DEFAULT_RPP).toInt
    val page = params.getOrElse(C.Api.Search.PAGE, "1").toInt

    val foldersQuery = params.getOrElse(C.Api.Search.FOLDERS, "")

    val folderId = foldersQuery.isEmpty match {
      case true => app.REPO.rootFolderId
      case false => foldersQuery
    }

    query(folderId, page, rpp)
  }

  get(s"/triage/p/:${C.Api.Search.PAGE}/rpp/:${C.Api.Search.RESULTS_PER_PAGE}") {
    val rpp = params.getOrElse(C.Api.Search.RESULTS_PER_PAGE, C.DEFAULT_RPP).toInt
    val page = params.getOrElse(C.Api.Search.PAGE, "1").toInt

    query(app.REPO.triageFolderId, page, rpp)
  }

  private def defaultQuery(folderId: String): ActionResult = {
    val q = Query(
      params = Map(C.Api.Folder.QUERY_ARG_NAME -> folderId),
      rpp = 20, page = 1)

    val results = app.service.library.query(q)

    Ok(Json.obj(
      C.Api.Search.ASSETS -> results.records.map { x =>
        val asset = x: Asset
        Util.withFormattedMetadata(app, asset)
      },
      C.Api.TOTAL_RECORDS -> results.total,
      C.Api.CURRENT_PAGE -> q.page,
      C.Api.TOTAL_PAGES -> results.totalPages,
      C.Api.RESULTS_PER_PAGE -> q.rpp
    ))
  }

  private def query(folderId: String, page: Int, rpp: Int): ActionResult = {
    val rpp = params.getOrElse(C.Api.Search.RESULTS_PER_PAGE, C.DEFAULT_RPP).toInt
    val page = params.getOrElse(C.Api.Search.PAGE, "1").toInt

    val q = Query(
      params = Map(C.Api.Folder.QUERY_ARG_NAME -> folderId),
      rpp = rpp, page = page)

    val results = app.service.library.query(q)

    Ok(Json.obj(
      C.Api.Search.ASSETS -> results.records.map { x =>
        val asset = x: Asset
        Util.withFormattedMetadata(app, asset)
      },
      C.Api.TOTAL_RECORDS -> results.total,
      C.Api.CURRENT_PAGE -> q.page,
      C.Api.TOTAL_PAGES -> results.totalPages,
      C.Api.RESULTS_PER_PAGE -> q.rpp
    ))
  }
}

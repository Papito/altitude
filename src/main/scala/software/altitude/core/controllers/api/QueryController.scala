package software.altitude.core.controllers.api

import org.scalatra.{ActionResult, Ok}
import org.slf4j.LoggerFactory
import play.api.libs.json.Json
import software.altitude.core.controllers.Util
import software.altitude.core.models.Asset
import software.altitude.core.util.{Query, SearchQuery}
import software.altitude.core.{Const => C}

class QueryController extends BaseApiController {
  private final val log = LoggerFactory.getLogger(getClass)

  get("/") {
    val foldersQuery = params.getOrElse(C.Api.Search.FOLDERS, "")

    val folderId = if (foldersQuery.isEmpty) repository.rootFolderId else foldersQuery

    defaultQuery(folderId)
  }

  get(s"/p/:${C.Api.Search.PAGE}/rpp/:${C.Api.Search.RESULTS_PER_PAGE}") {
    val rpp = params.getOrElse(C.Api.Search.RESULTS_PER_PAGE, C.DEFAULT_RPP).toInt
    val page = params.getOrElse(C.Api.Search.PAGE, "1").toInt

    val foldersQuery = params.getOrElse(C.Api.Search.FOLDERS, "")

    val folderIdsArg = if (foldersQuery.isEmpty) repository.rootFolderId else foldersQuery

    val q = new SearchQuery(
      rpp = rpp, page = page,
      folderIds = Util.parseFolderIds(folderIds = folderIdsArg)
    )

    query(q)
  }

  get("/triage") {
    defaultQuery(repository.triageFolderId)
  }

  get(s"/triage/p/:${C.Api.Search.PAGE}/rpp/:${C.Api.Search.RESULTS_PER_PAGE}") {
    val rpp = params.getOrElse(C.Api.Search.RESULTS_PER_PAGE, C.DEFAULT_RPP).toInt
    val page = params.getOrElse(C.Api.Search.PAGE, "1").toInt

    val q = new SearchQuery(
      rpp = rpp, page = page,
      folderIds = Set(repository.triageFolderId)
    )

    query(q)
  }

  get(s"/search/p/:${C.Api.Search.PAGE}/rpp/:${C.Api.Search.RESULTS_PER_PAGE}") {
    // FIXME: any Scalatra way to enforce these are ints?
    val rpp = params.getOrElse(C.Api.Search.RESULTS_PER_PAGE, C.DEFAULT_RPP).toInt
    val page = params.getOrElse(C.Api.Search.PAGE, "1").toInt

    val queryString = params.get(C.Api.Search.QUERY_TEXT)
    log.debug(s"Query string: $queryString")
  }

  private def defaultQuery(folderId: String): ActionResult = {
    val q = new SearchQuery(
      rpp = C.DEFAULT_RPP.toInt,
      folderIds = Set(folderId)
    )

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

  private def query(q: Query): ActionResult = {
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

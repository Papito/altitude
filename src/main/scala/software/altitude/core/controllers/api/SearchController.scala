package software.altitude.core.controllers.api

import org.scalatra.ActionResult
import org.scalatra.Ok
import play.api.libs.json.JsNull
import play.api.libs.json.Json
import software.altitude.core.controllers.BaseApiController
import software.altitude.core.models.{Asset, Repository, User}
import software.altitude.core.util.SearchQuery
import software.altitude.core.util.SearchSort
import software.altitude.core.{RequestContext, Const => C}

class SearchController extends BaseApiController {

  get("/") {
    app.service.repository.setContextFromUserActiveRepo(RequestContext.getAccount)
    val repo: Repository = RequestContext.getRepository
    val foldersQuery = params.getOrElse(C.Api.Search.FOLDERS, "")

    val folderId = if (foldersQuery.isEmpty) repo.rootFolderId else foldersQuery

    defaultQuery(folderId)
  }

  get(s"/p/:${C.Api.Search.PAGE}/rpp/:${C.Api.Search.RESULTS_PER_PAGE}/?") {
    app.service.repository.setContextFromUserActiveRepo(RequestContext.getAccount)
    val repo: Repository = RequestContext.getRepository

    val rpp = params.getOrElse(C.Api.Search.RESULTS_PER_PAGE, C.Api.Search.DEFAULT_RPP.toString).toInt
    val page = params.getOrElse(C.Api.Search.PAGE, "1").toInt
    val queryText = params.get(C.Api.Search.QUERY_TEXT)
    val sortArg = params.get(C.Api.Search.SORT)
    logger.info(s"Query string: $queryText")

    logger.info(s"Sort: $sortArg")

    val sort: List[SearchSort] = if (sortArg.isDefined) {
      val fieldId :: directionInt :: _ = sortArg.get.split("\\|").toList
      logger.info(s"Sort field ID: $fieldId, direction: $directionInt")
      List()
    } else {
      List()
    }

    // TODO: if there is query text, do not specify folder ids. Search everything
    val foldersQuery = params.getOrElse(C.Api.Search.FOLDERS, "")
    val folderIdsArg = if (foldersQuery.isEmpty) repo.rootFolderId else foldersQuery

    val q = new SearchQuery(
      text = queryText,
      rpp = rpp, page = page,
      folderIds = parseFolderIds(folderIds = folderIdsArg),
      searchSort = sort
    )

    query(q)
  }

  get("/triage") {
  }

  get(s"/triage/p/:${C.Api.Search.PAGE}/rpp/:${C.Api.Search.RESULTS_PER_PAGE}") {
  }

  private def defaultQuery(folderId: String): ActionResult = {
    val q = new SearchQuery(
      rpp = C.Api.Search.DEFAULT_RPP,
      folderIds = Set(folderId)
    )

    val results = app.service.library.search(q)

    Ok(Json.obj(
      C.Api.Search.ASSETS -> results.records.map { x =>
        val asset = x: Asset
        asset.metadata.toJson
      },
      C.Api.TOTAL_RECORDS -> results.total,
      C.Api.CURRENT_PAGE -> q.page,
      C.Api.TOTAL_PAGES -> results.totalPages,
      C.Api.RESULTS_PER_PAGE -> q.rpp,
      C.Api.Search.SORT -> (if (results.sort.isEmpty) JsNull else results.sort.head.toJson)
    ))
  }

  private def query(q: SearchQuery): ActionResult = {
    val results = app.service.library.search(q)

    Ok(Json.obj(
      C.Api.Search.ASSETS -> results.records.map { x =>
        val asset = x: Asset
        asset.metadata.toJson
      },
      C.Api.TOTAL_RECORDS -> results.total,
      C.Api.CURRENT_PAGE -> q.page,
      C.Api.TOTAL_PAGES -> results.totalPages,
      C.Api.RESULTS_PER_PAGE -> q.rpp,
      C.Api.Search.SORT -> (if (results.sort.isEmpty) JsNull else results.sort.head.toJson)
    ))
  }

  private def parseFolderIds(folderIds: String): Set[String] = {
    if (folderIds.isEmpty) {
      Set[String]()
    }
    else {
      folderIds
        .split(s"\\${C.Api.MULTI_VALUE_DELIM}")
        .map(_.trim)
        .filter(_.nonEmpty).toSet
    }
  }
}

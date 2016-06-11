package altitude.controllers.api

import altitude.models.search.Query
import altitude.{Const => C}
import altitude.Validators.ApiValidator
import org.scalatra.Ok
import org.slf4j.LoggerFactory
import play.api.libs.json.Json

class TrashController extends BaseApiController {

  private final val log = LoggerFactory.getLogger(getClass)

  post(s"/recycle/:id") {
    val id = params.get(C("Api.ID")).get
    log.info(s"Moving $id to TRASH")
    app.service.library.moveToTrash(id)

    OK
  }

  post(s"/recycle") {
    log.info(s"Deleting assets")

    val validator = ApiValidator(List(C("Api.Folder.ASSET_IDS")))
    validator.validate(requestJson.get)

    val assetIds = (requestJson.get \ C("Api.Folder.ASSET_IDS")).as[Set[String]]

    log.debug(s"Assets to move to trash: $assetIds")

    app.service.library.moveAssetsToTrash(assetIds)

    OK
  }

  get(s"/p/:${C("Api.Search.PAGE")}/rpp/:${C("Api.Search.RESULTS_PER_PAGE")}") {
    // FIXME: magic constants
    val rpp = this.params.getOrElse(C("Api.Search.RESULTS_PER_PAGE"), "20").toInt
    // FIXME: magic constants
    val page = this.params.getOrElse(C("Api.Search.PAGE"), "1").toInt

    val q = Query(rpp = rpp, page = page)

    val results = app.service.trash.query(q)

    Ok(Json.obj(
      C("Api.Search.ASSETS") -> results.records,
      C("Api.TOTAL_RECORDS") -> results.total,
      C("Api.CURRENT_PAGE") -> q.page,
      C("Api.TOTAL_PAGES") -> results.totalPages,
      C("Api.RESULTS_PER_PAGE") -> results.query.rpp
    ))
  }
}

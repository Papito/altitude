package altitude.controllers.api

import altitude.controllers.BaseController
import altitude.models.search.Query
import altitude.{Const => C}
import org.fusesource.scalate.servlet.Config
import org.scalatra.Ok
import org.slf4j.LoggerFactory
import play.api.libs.json.Json

class SearchController extends BaseApiController {
  private final val log = LoggerFactory.getLogger(getClass)

  get("/") {
    val queryString = this.params.getOrElse(C.Api.Search.QUERY_STRING, "")
    val rpp = this.params.getOrElse(C.Api.Search.RESULTS_PER_PAGE, "20").toInt
    val page = this.params.getOrElse(C.Api.Search.PAGE, "1").toInt

    val q = Query(rpp = rpp, page = page)
    val assets = app.service.library.search(q)
    val jsonAssets = for (asset <- assets) yield asset.toJson

    Ok(Json.obj(
      C.Api.Search.RESULT_BOX_SIZE -> app.config.getInt("result.box.pixels"),
      C.Api.Search.ASSETS -> jsonAssets
    ))
  }

  get("/meta/box") {
    Ok(Json.obj(
      C.Api.Search.RESULT_BOX_SIZE -> app.config.getInt("result.box.pixels")
    ))
  }
}

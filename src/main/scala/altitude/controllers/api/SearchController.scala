package altitude.controllers.api

import altitude.controllers.BaseController
import altitude.models.search.Query
import altitude.{Const => C}
import org.scalatra.Ok
import org.slf4j.LoggerFactory
import play.api.libs.json.Json

class SearchController extends BaseApiController {
  private final val log = LoggerFactory.getLogger(getClass)

  get("/") {
    //val queryString = this.params.getOrElse(C.Api.Search.QUERY_STRING, "")

    val assets = app.service.library.search(new Query())
    val jsonAssets = for (asset <- assets) yield asset.toJson

    Ok(Json.obj(
      C.Api.Search.RESULT_BOX_SIZE -> app.config.getInt("result.box.pixels"),
      C.Api.Search.ASSETS -> jsonAssets
    ))
  }
}

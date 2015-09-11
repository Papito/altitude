package altitude.controllers.api

import altitude.controllers.BaseController
import altitude.models.search.Query
import altitude.{Const => C}
import org.scalatra.Ok
import play.api.libs.json.Json

class SearchController extends BaseApiController {
  get("/") {
    val assets = app.service.library.search(new Query())
    val jsonAssets = for (asset <- assets) yield asset.toJson
    Ok(Json.obj(C.Api.Search.ASSETS -> jsonAssets))
  }
}

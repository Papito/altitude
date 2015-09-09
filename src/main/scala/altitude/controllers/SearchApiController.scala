package altitude.controllers

import altitude.models.{Asset, Preview}
import altitude.models.search.Query
import org.scalatra.Ok
import play.api.libs.json.{Json, JsArray, JsObject}
import altitude.{Const => C}

class SearchApiController extends BaseController {
  get("/") {
    val assets = app.service.library.search(new Query())
    val jsonAssets = for (asset <- assets) yield asset.toJson
    Ok(Json.obj(C.Api.Search.ASSETS -> jsonAssets))
  }
}

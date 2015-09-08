package altitude.controllers

import org.scalatra.Ok
import play.api.libs.json.Json

class ImportProfileApiController extends BaseController {
  get("/") {
    Ok()
  }
}

package altitude.controllers.api

import org.slf4j.LoggerFactory
import org.scalatra.Ok
import play.api.libs.json.{JsArray, Json}
import altitude.{Const => C}

class TagConfigController extends BaseApiController {
  override val log =  LoggerFactory.getLogger(getClass)

  get("/") {
    val tagConfig = app.service.tagConfig.getAll

    Ok(Json.obj(
      C.Api.TagConfig.TAG_CONFIG -> JsArray(tagConfig.map(_.toJson))
    ))
  }
}

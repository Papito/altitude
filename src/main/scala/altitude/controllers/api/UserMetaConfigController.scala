package altitude.controllers.api

import org.slf4j.LoggerFactory
import org.scalatra.Ok
import play.api.libs.json.{JsArray, Json}
import altitude.{Const => C}

class UserMetaConfigController extends BaseApiController {
  override val log =  LoggerFactory.getLogger(getClass)

  get("/") {
    val userMetaConfig = app.service.userMetaFieldConfig.getAll

    Ok(Json.obj(
      C.Api.UserMetaField.METAFIELDS -> JsArray(userMetaConfig.map(_.toJson))
    ))
  }
}

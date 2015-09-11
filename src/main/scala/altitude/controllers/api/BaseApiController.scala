package altitude.controllers.api

import altitude.controllers.BaseController
import altitude.exceptions.ValidationException
import org.scalatra.{BadRequest, NotFound}
import org.slf4j.LoggerFactory
import play.api.libs.json.{JsObject, JsString, Json}

class BaseApiController extends BaseController {
  val log =  LoggerFactory.getLogger(getClass)

  before() {
    contentType = "application/json"
  }

  notFound {
    NotFound()
  }

  error {
    case ex: ValidationException => {
      val json: JsObject = Json.obj()
      val out = ex.errors.keys.fold(json){(res, field) => {
        val key = field.toString
        json ++ Json.obj(key -> ex.errors(key))
      }}.asInstanceOf[JsObject]
      BadRequest(out.toString())
    }
  }
}

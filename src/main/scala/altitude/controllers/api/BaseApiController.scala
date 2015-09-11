package altitude.controllers.api

import altitude.controllers.BaseController
import altitude.exceptions.ValidationException
import org.scalatra.{BadRequest, NotFound}
import org.slf4j.LoggerFactory
import play.api.libs.json.Json

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
      BadRequest(
        ex.errors.keys.foldLeft(Json.obj()){(res, field) => {
          val key = field.toString
          res ++ Json.obj(key -> ex.errors(key))
        }}
      )
    }
  }
}

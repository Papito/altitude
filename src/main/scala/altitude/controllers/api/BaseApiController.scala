package altitude.controllers.api

import altitude.controllers.BaseController
import altitude.exceptions.ValidationException
import org.scalatra.{GZipSupport, BadRequest, NotFound}
import org.slf4j.LoggerFactory
import play.api.libs.json.Json
import altitude.{Const => C}

class BaseApiController extends BaseController with GZipSupport {
  val log =  LoggerFactory.getLogger(getClass)

  before() {
    contentType = "application/json"
  }

  notFound {
    NotFound()
  }

  error {
    case ex: ValidationException => {
      val jsonErrors = ex.errors.keys.foldLeft(Json.obj()){(res, field) => {
        val key = field.toString
        res ++ Json.obj(key -> ex.errors(key))}
      }

      BadRequest(Json.obj(C.Api.VALIDATION_ERRORS -> jsonErrors))
    }
  }
}

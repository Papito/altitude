package altitude.controllers.api

import altitude.Validators.ApiValidator
import altitude.controllers.BaseController
import altitude.exceptions.ValidationException
import org.scalatra.{GZipSupport, BadRequest, NotFound}
import org.slf4j.LoggerFactory
import play.api.libs.json.Json
import altitude.{Const => C}

class BaseApiController extends BaseController with GZipSupport {
  private final val log = LoggerFactory.getLogger(getClass)
  val HTTP_POST_VALIDATOR: Option[ApiValidator] = None

  before() {
    contentType = "application/json"

    HTTP_POST_VALIDATOR match {
      case Some(ApiValidator(required)) if request.getMethod.toLowerCase == "post" => HTTP_POST_VALIDATOR.get.validate(params)
      case _ =>
    }
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

      BadRequest(Json.obj(
        C.Api.ERROR -> ex.message,
        C.Api.VALIDATION_ERRORS -> jsonErrors))
    }
  }
}

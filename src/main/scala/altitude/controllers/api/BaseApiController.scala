package altitude.controllers.api

import java.io.{PrintWriter, StringWriter}

import altitude.Validators.ApiValidator
import altitude.controllers.BaseController
import altitude.exceptions.ValidationException
import org.scalatra.{InternalServerError, GZipSupport, BadRequest, NotFound}
import org.slf4j.LoggerFactory
import play.api.libs.json.{JsString, Json}
import altitude.{Const => C}

class BaseApiController extends BaseController with GZipSupport {
  private final val log = LoggerFactory.getLogger(getClass)
  val HTTP_POST_VALIDATOR: Option[ApiValidator] = None
  val HTTP_DELETE_VALIDATOR: Option[ApiValidator] = None
  val HTTP_UPDATE_VALIDATOR: Option[ApiValidator] = None

  before() {
    log.info(
      s"API ${request.getRequestURI} ${request.getMethod.toUpperCase} request with parameters ${request.getParameterMap}")

    contentType = "application/json; charset=UTF-8"

    /*
    Process all validators that may be set for this request, per method.
     */
    HTTP_POST_VALIDATOR match {
      case Some(ApiValidator(required)) if request.getMethod.toLowerCase == "post" => HTTP_POST_VALIDATOR.get.validate(params)
      case _ if request.getMethod.toLowerCase == "post" =>
        log.warn(s"No POST validator specified for ${this.getClass.getName}")
      case _ =>
    }

    HTTP_DELETE_VALIDATOR match {
      case Some(ApiValidator(fields)) if request.getMethod.toLowerCase == "delete" => HTTP_DELETE_VALIDATOR.get.validate(params)
      case _ if request.getMethod.toLowerCase == "delete" =>
        log.warn(s"No DELETE validator specified for ${this.getClass.getName}")
      case _ =>
    }

    HTTP_UPDATE_VALIDATOR match {
      case Some(ApiValidator(required)) if request.getMethod.toLowerCase == "upodate" => HTTP_UPDATE_VALIDATOR.get.validate(params)
      case _ if request.getMethod.toLowerCase == "update"  =>
        log.warn(s"No UPDATE validator specified for ${this.getClass.getName}")
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
    case ex: Exception => {
      ex.printStackTrace()
      val sw: StringWriter = new StringWriter()
      val pw: PrintWriter = new PrintWriter(sw)
      ex.printStackTrace(pw)

      log.error(s"${ex.getClass.getName} exception: ${sw.toString}")

      InternalServerError(Json.obj(
        C.Api.ERROR -> (if (ex.getMessage!= null) ex.getMessage else ex.getClass.getName),
        C.Api.STACKTRACE -> sw.toString))
    }
  }
}

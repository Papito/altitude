package altitude.controllers.api

import java.io.{PrintWriter, StringWriter}

import altitude.Validators.ApiValidator
import altitude.controllers.BaseController
import altitude.exceptions.ValidationException
import altitude.{Const => C}
import org.scalatra.{BadRequest, GZipSupport, InternalServerError, NotFound}
import org.slf4j.LoggerFactory
import play.api.libs.json.{JsNull, Json}

class BaseApiController extends BaseController with GZipSupport {
  private final val log = LoggerFactory.getLogger(getClass)
  val HTTP_POST_VALIDATOR: Option[ApiValidator] = None
  val HTTP_DELETE_VALIDATOR: Option[ApiValidator] = None
  val HTTP_UPDATE_VALIDATOR: Option[ApiValidator] = None

  before() {
    log.info(
      s"API ${request.getRequestURI} ${request.getMethod.toUpperCase} request with parameters ${request.getParameterMap}")

    checkPayload()

    contentType = "application/json; charset=UTF-8"

    /*
    Process all validators that may be set for this request, per method.
     */
    HTTP_POST_VALIDATOR match {
      case Some(ApiValidator(required)) if request.getMethod.toLowerCase == "post" => HTTP_POST_VALIDATOR.get.validateForm(params)
      case _ if request.getMethod.toLowerCase == "post" =>
        log.debug(s"No POST validator specified for ${this.getClass.getName}")
      case _ =>
    }

    HTTP_DELETE_VALIDATOR match {
      case Some(ApiValidator(fields)) if request.getMethod.toLowerCase == "delete" => HTTP_DELETE_VALIDATOR.get.validateForm(params)
      case _ if request.getMethod.toLowerCase == "delete" =>
        log.debug(s"No DELETE validator specified for ${this.getClass.getName}")
      case _ =>
    }

    HTTP_UPDATE_VALIDATOR match {
      case Some(ApiValidator(required)) if request.getMethod.toLowerCase == "upodate" => HTTP_UPDATE_VALIDATOR.get.validateForm(params)
      case _ if request.getMethod.toLowerCase == "update"  =>
        log.debug(s"No UPDATE validator specified for ${this.getClass.getName}")
      case _ =>
    }
  }

  // override to disable this check in controllers that do not require a JSON payload for post and put
  private def checkPayload(): Unit = {
    if (List("post", "put").contains(request.getMethod.toLowerCase) && request.body.isEmpty) {
      throw ValidationException(C("msg.err.empty_request_body"))
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
        C("Api.VALIDATION_ERROR") -> ex.message,
        C("Api.VALIDATION_ERRORS") -> (if (ex.errors.isEmpty) JsNull else jsonErrors)
      ))
    }
    case ex: Exception => {
      ex.printStackTrace()
      val sw: StringWriter = new StringWriter()
      val pw: PrintWriter = new PrintWriter(sw)
      ex.printStackTrace(pw)

      log.error(s"${ex.getClass.getName} exception: ${sw.toString}")

      InternalServerError(Json.obj(
        C("Api.ERROR") -> (if (ex.getMessage!= null) ex.getMessage else ex.getClass.getName),
        C("Api.STACKTRACE") -> sw.toString))
    }
  }
}

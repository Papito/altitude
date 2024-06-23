package software.altitude.core.controllers.api

import org.scalatra._
import org.slf4j.LoggerFactory
import play.api.libs.json.JsNull
import play.api.libs.json.JsObject
import play.api.libs.json.Json
import software.altitude.core.DataScrubber
import software.altitude.core.NotFoundException
import software.altitude.core.ValidationException
import software.altitude.core.Validators.ApiRequestValidator
import software.altitude.core.controllers.BaseController
import software.altitude.core.{Const => C}

import java.lang.System.currentTimeMillis

class BaseApiController extends BaseController {
  private final val log = LoggerFactory.getLogger(getClass)

  val OK: ActionResult = Ok("{}")

  def requestJson: Option[JsObject] = Some(
    if (request.body.isEmpty) Json.obj() else Json.parse(request.body).as[JsObject]
  )

  private def requestMethod: String = request.getMethod.toLowerCase

  before() {
    contentType = "application/json; charset=UTF-8"

    // verify that requests with request body are not empty
     checkPayload()
  }

  override def logRequestStart(): Unit = log.info(
      s"API ${request.getRequestURI} ${requestMethod.toUpperCase}, Body {${request.body}} Args: ${request.getParameterMap}"
  )

  override def logRequestEnd(): Unit = {
    val startTime: Long = request.getAttribute("startTime").asInstanceOf[Long]
    log.info(s"API request END: ${request.getRequestURI} in ${currentTimeMillis - startTime}ms")
  }

  // override to disable this check in controllers that do not require a JSON payload for post and put
  private def checkPayload(): Unit = {
    if (List("post", "put").contains(requestMethod) && request.body.isEmpty) {
      throw ValidationException(C.Msg.Err.EMPTY_REQUEST_BODY)
    }
  }

  def scrubAndValidatedJson(dataScrubber: Option[DataScrubber] = None,
                            apiRequestValidator: Option[ApiRequestValidator] = None): JsObject = {
    val scrubbedJson = if (dataScrubber.isDefined) dataScrubber.get.scrub(requestJson.get) else requestJson.get
    if (apiRequestValidator.isDefined) apiRequestValidator.get.validate(scrubbedJson)
    scrubbedJson
  }

  error {
    case ex: ValidationException =>
      val jsonErrors = ex.errors.keys.foldLeft(Json.obj()) {(res, field) => {
        val key = field
        res ++ Json.obj(key -> ex.errors(key))}
      }

      BadRequest(Json.obj(
        C.Api.VALIDATION_ERROR -> ex.message,
        C.Api.VALIDATION_ERRORS -> (if (ex.errors.isEmpty) JsNull else jsonErrors)
      ))
    case _: NotFoundException =>
      NotFound(Json.obj())
    case ex: Exception =>
      val strStacktrace = software.altitude.core.Util.logStacktrace(ex)

      InternalServerError(Json.obj(
        C.Api.ERROR -> (if (ex.getMessage!= null) ex.getMessage else ex.getClass.getName),
        C.Api.STACKTRACE -> strStacktrace))
  }

  override def setUser(): Unit = {
  }

  override def setRepository(): Unit = {
  }
}

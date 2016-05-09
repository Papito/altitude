package altitude.controllers.api

import java.io.{PrintWriter, StringWriter}

import altitude.Validators.ApiValidator
import altitude.controllers.BaseController
import altitude.exceptions.ValidationException
import altitude.{Const => C}
import org.scalatra._
import org.slf4j.LoggerFactory
import play.api.libs.json.{JsObject, JsNull, Json}

class BaseApiController extends BaseController with GZipSupport {
  private final val log = LoggerFactory.getLogger(getClass)

  val OK = Ok("{}")

  val HTTP_POST_VALIDATOR: Option[ApiValidator] = None
  val HTTP_DELETE_VALIDATOR: Option[ApiValidator] = None
  val HTTP_UPDATE_VALIDATOR: Option[ApiValidator] = None

  var requestJson: Option[JsObject] = None

  def requestMethod = request.getMethod.toLowerCase

  before() {
    log.info(
      s"API ${request.getRequestURI} ${requestMethod.toUpperCase} request with ${request.body}")

    // verify that requests with request body are not empty
    checkPayload()

    requestJson = Some(
      if (request.body.isEmpty) Json.obj() else Json.parse(request.body).as[JsObject]
    )

    /*
    Process all validators that may be set for this controller/method.
     */
    HTTP_POST_VALIDATOR match {
      case Some(ApiValidator(required)) if requestMethod == "post" => HTTP_POST_VALIDATOR.get.validate(requestJson.get)
      case _ if requestMethod == "post" =>
        log.debug(s"No POST validator specified for ${this.getClass.getName}")
      case _ =>
    }

    HTTP_DELETE_VALIDATOR match {
      case Some(ApiValidator(fields)) if requestMethod == "delete" => HTTP_DELETE_VALIDATOR.get.validate(requestJson.get)
      case _ if requestMethod == "delete" =>
        log.debug(s"No DELETE validator specified for ${this.getClass.getName}")
      case _ =>
    }

    HTTP_UPDATE_VALIDATOR match {
      case Some(ApiValidator(required)) if requestMethod == "put" => HTTP_UPDATE_VALIDATOR.get.validate(requestJson.get)
      case _ if requestMethod == "update"  =>
        log.debug(s"No PUT validator specified for ${this.getClass.getName}")
      case _ =>
    }

    // all responses are of type:
    contentType = "application/json; charset=UTF-8"
  }

  // override to disable this check in controllers that do not require a JSON payload for post and put
  private def checkPayload(): Unit = {
    if (List("post", "put").contains(requestMethod) && request.body.isEmpty) {
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

package altitude.controllers.api

import altitude.Validators.ApiValidator
import altitude.controllers.BaseController
import altitude.exceptions.{NotFoundException, ValidationException}
import altitude.models.User
import altitude.{Const => C}
import org.scalatra._
import org.slf4j.LoggerFactory
import play.api.libs.json.{JsNull, JsObject, Json}

class BaseApiController extends BaseController with GZipSupport {
  private final val log = LoggerFactory.getLogger(getClass)

  val OK = Ok("{}")

  val HTTP_POST_VALIDATOR: Option[ApiValidator] = None
  val HTTP_DELETE_VALIDATOR: Option[ApiValidator] = None
  val HTTP_UPDATE_VALIDATOR: Option[ApiValidator] = None

  def requestJson: Option[JsObject] = Some(
    // FIXME: cache this in the request itself
    if (request.body.isEmpty) Json.obj() else Json.parse(request.body).as[JsObject]
  )

  def requestMethod = request.getMethod.toLowerCase

  implicit def user = if (request.contains("user"))
      request.getAttribute("user").asInstanceOf[User]
    else
      throw new RuntimeException("User was not set for this request")

  before() {
    log.info(
      s"API ${request.getRequestURI} ${requestMethod.toUpperCase} request with {${request.body}} and ${request.getParameterMap}")

    // verify that requests with request body are not empty
    checkPayload()
    setUser()

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
      throw ValidationException(C.MSG("err.empty_request_body"))
    }
  }

  private def setUser(): Unit = {
    val user = User(id = Some("1"), rootFolderId = "0", uncatFolderId = "1")
    log.info(s"User for this request $user")
    request.setAttribute("user", user)
    /*
    user = requestJson.isDefined match {
      case true => {
        val id = (requestJson.get \ C.Api.USER_ID).as[String]
        User(id = Some(id), rootFolderId = "0", uncatFolderId = "1")
      }
      case false => {
        val id = request.get(C.Api.USER_ID).toString
        User(id = Some(id), rootFolderId = "0", uncatFolderId = "1")
      }
    }
  */
  }

  error {
    case ex: ValidationException => {
      val jsonErrors = ex.errors.keys.foldLeft(Json.obj()){(res, field) => {
        val key = field.toString
        res ++ Json.obj(key -> ex.errors(key))}
      }

      BadRequest(Json.obj(
        C.Api.VALIDATION_ERROR -> ex.message,
        C.Api.VALIDATION_ERRORS -> (if (ex.errors.isEmpty) JsNull else jsonErrors)
      ))
    }
    case ex: NotFoundException => {
      NotFound(Json.obj())
    }
    case ex: Exception => {
      val strStacktrace = altitude.Util.logStacktrace(ex)

      InternalServerError(Json.obj(
        C.Api.ERROR -> (if (ex.getMessage!= null) ex.getMessage else ex.getClass.getName),
        C.Api.STACKTRACE -> strStacktrace))
    }
  }
}

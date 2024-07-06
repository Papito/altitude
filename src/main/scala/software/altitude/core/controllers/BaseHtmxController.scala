package software.altitude.core.controllers

import org.scalatra._
import org.scalatra.scalate.ScalateSupport
import play.api.libs.json.JsObject
import play.api.libs.json.Json
import software.altitude.core.ValidationException
import software.altitude.core.{Const => C}

import java.lang.System.currentTimeMillis

class BaseHtmxController extends BaseController with ScalateSupport {
  val OK: ActionResult = Ok("{}")

  def unscrubbedReqJson: Option[JsObject] = Some(
    if (request.body.isEmpty) Json.obj() else Json.parse(request.body).as[JsObject]
  )

  private def requestMethod: String = request.getMethod.toLowerCase

  before() {
    contentType = "application/json; charset=UTF-8"

    // verify that requests with request body are not empty
    checkPayload()
  }

  override def logRequestStart(): Unit = logger.info(
    s"API ${request.getRequestURI} ${requestMethod.toUpperCase}, Body {${request.body}} Args: ${request.getParameterMap}"
  )

  override def logRequestEnd(): Unit = {
    val startTime: Long = request.getAttribute("startTime").asInstanceOf[Long]
    logger.info(s"API request END: ${request.getRequestURI} in ${currentTimeMillis - startTime}ms")
  }

  // override to disable this check in controllers that do not require a JSON payload for post and put
  private def checkPayload(): Unit = {
    if (List("post", "put").contains(requestMethod) && request.body.isEmpty) {
      throw ValidationException(C.Msg.Err.EMPTY_REQUEST_BODY)
    }
  }

  override def setUser(): Unit = {
  }

  override def setRepository(): Unit = {
  }
}

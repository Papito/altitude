package software.altitude.core.controllers

import org.scalatra._
import org.scalatra.scalate.ScalateSupport
import play.api.libs.json.JsObject
import play.api.libs.json.Json
import software.altitude.core.{RequestContext, ValidationException, Const => C}

import java.lang.System.currentTimeMillis

class BaseHtmxController extends BaseController with ScalateSupport {
  val OK: ActionResult = Ok("{}")

  def unscrubbedReqJson: Option[JsObject] = {
    if (request.contentType.isDefined && request.contentType.get != "application/json") {
      throw ValidationException(C.Msg.Err.INVALID_CONTENT_TYPE)
    }

    Some(if (request.body.isEmpty) Json.obj() else Json.parse(request.body).as[JsObject])
  }

  private def requestMethod: String = request.getMethod.toLowerCase

  override def logRequestStart(): Unit = logger.info(
    s"API ${request.getRequestURI} ${requestMethod.toUpperCase}, Body {${request.body}} Args: ${request.getParameterMap}"
  )

  override def logRequestEnd(): Unit = {
    val startTime: Long = request.getAttribute("startTime").asInstanceOf[Long]
    logger.info(s"HTMX request END: ${request.getRequestURI} in ${currentTimeMillis - startTime}ms")
    logger.info(s"HTMX request READ queries: ${RequestContext.readQueryCount.value}")
    logger.info(s"HTMX request WRITE queries: ${RequestContext.writeQueryCount.value}")
  }
}

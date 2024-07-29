package software.altitude.core.controllers

import org.scalatra.ContentEncodingSupport
import org.scalatra.InternalServerError
import org.scalatra.ScalatraServlet
import org.scalatra.UrlGeneratorSupport
import org.scalatra.scalate.ScalateUrlGeneratorSupport
import org.slf4j.MDC
import software.altitude.core.{AltitudeServletContext, Const, RequestContext}
import software.altitude.core.auth.AuthenticationSupport
import software.altitude.core.models.Repository
import software.altitude.core.models.User

import java.io.PrintWriter
import java.io.StringWriter
import java.lang.System.currentTimeMillis

abstract class BaseController
  extends ScalatraServlet
    with ContentEncodingSupport
    with UrlGeneratorSupport
    with ScalateUrlGeneratorSupport
    with AuthenticationSupport
    with AltitudeServletContext {

  before() {
    val requestId = software.altitude.core.Util.randomStr(size = 6)
    request.setAttribute("request_id", requestId)
    MDC.put("REQUEST_ID", s"[$requestId]")
    request.setAttribute("startTime", currentTimeMillis)
    logRequestStart()
  }

  after() {
    logRequestEnd()
    MDC.clear()
    RequestContext.clear()
  }

  protected def logRequestStart(): Unit = {
    if (isAssetRequest) return

    logger.info(s"Request START: ${request.getRequestURI} args: {${request.getParameterMap}}")
  }

  protected def logRequestEnd(): Unit = {
    if (isAssetRequest) return

    val startTime: Long = request.getAttribute("startTime").asInstanceOf[Long]
    logger.info(s"Request END (${response.status}): ${request.getRequestURI} in ${currentTimeMillis - startTime}ms")
    logger.info(s"Request READ queries: ${RequestContext.readQueryCount.value}")
    logger.info(s"Request WRITE queries: ${RequestContext.writeQueryCount.value}")
  }

  private def isAssetRequest =  request.pathInfo.startsWith("/css") ||
    request.pathInfo.startsWith("/js") ||
    request.pathInfo.startsWith("/webfonts") ||
    request.pathInfo.startsWith("/images")
//    request.pathInfo.startsWith(s"/${Const.DataStore.PREVIEW}")

  error {
    case ex: Exception =>
      ex.printStackTrace()
      val sw: StringWriter = new StringWriter()
      val pw: PrintWriter = new PrintWriter(sw)
      ex.printStackTrace(pw)
      logger.error(s"Exception ${sw.toString}")
      InternalServerError(sw.toString)
  }
}

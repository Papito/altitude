package altitude.controllers

import java.io.{PrintWriter, StringWriter}

import altitude.{Util, SingleApplication}
import altitude.models.User
import org.scalatra.InternalServerError
import org.slf4j.{MDC, LoggerFactory}

import scala.compat.Platform

abstract class BaseController extends AltitudeStack with SingleApplication {
  private final val log = LoggerFactory.getLogger(getClass)

  error {
    case ex: Exception => {
      ex.printStackTrace()
      val sw: StringWriter = new StringWriter()
      val pw: PrintWriter = new PrintWriter(sw)
      ex.printStackTrace(pw)
      log.error(s"Exception ${sw.toString}")
      InternalServerError(sw.toString)
    }
  }

  implicit def user = if (request.contains("user"))
    request.getAttribute("user").asInstanceOf[User]
  else
    throw new RuntimeException("User was not set for this request")

  before() {
    request.setAttribute("request_id", Util.randomStr(size = 6))
    MDC.put("REQUEST_ID", s"<${request.getAttribute("request_id").toString}>")
    request.setAttribute("startTime", Platform.currentTime)
    setUser()
    logRequestStart()
  }

  after() {
    logRequestEnd()
    MDC.clear()
  }

  protected def logRequestStart() = {
    log.debug(s"Request START: ${request.getRequestURI} args: {${request.getParameterMap}}")
  }

  protected def logRequestEnd() = {
    val startTime: Long = request.getAttribute("startTime").asInstanceOf[Long]
    log.debug(s"Request END: ${request.getRequestURI} in ${Platform.currentTime - startTime}ms")
  }

  protected def setUser() = {
    val user = User(id = Some("1"), rootFolderId = "0", uncatFolderId = "1")
    request.setAttribute("user", user)
    MDC.put("USER", s"[U: ${user.toString}]")
  }
}

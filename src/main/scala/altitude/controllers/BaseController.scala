package altitude.controllers

import java.io.{PrintWriter, StringWriter}

import altitude.{Const => C}
import altitude.models.User
import altitude.{Context, SingleApplication, Util}
import org.scalatra.InternalServerError
import org.slf4j.{LoggerFactory, MDC}

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

  def user = if (request.contains("user"))
    request.getAttribute("user").asInstanceOf[User]
  else
    throw new RuntimeException("User was not set for this request")

  implicit def context: Context = new Context(repo = C.REPO, user = user)

  before() {
    val requestId = Util.randomStr(size = 6)
    request.setAttribute("request_id", requestId)
    MDC.put("REQUEST_ID", s"[$requestId]")
    request.setAttribute("startTime", Platform.currentTime)
    setUser()
    logRequestStart()
  }

  after() {
    logRequestEnd()
    MDC.clear()
  }

  protected def logRequestStart() = {
    log.info(s"Request START: ${request.getRequestURI} args: {${request.getParameterMap}}")
  }

  protected def logRequestEnd() = {
    val startTime: Long = request.getAttribute("startTime").asInstanceOf[Long]
    log.info(s"Request END: ${request.getRequestURI} in ${Platform.currentTime - startTime}ms")
  }

  protected def setUser() = {
    val user = C.USER

    request.setAttribute("user", user)
    MDC.put("USER", s"[${user.toString}]")
  }
}

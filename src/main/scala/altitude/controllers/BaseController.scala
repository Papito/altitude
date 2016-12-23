package altitude.controllers

import java.io.{PrintWriter, StringWriter}

import altitude.models.{Repository, User}
import altitude.{Context, SingleApplication, Util}
import org.scalatra.InternalServerError
import org.slf4j.{LoggerFactory, MDC}

import scala.compat.Platform

abstract class BaseController extends AltitudeStack with SingleApplication {
  private final val log = LoggerFactory.getLogger(getClass)

  error {
    // TODO: Handling of errors is different from outside of development
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

  // FIXME: cache in the request
  implicit def context: Context = new Context(
    repo = new Repository(name = "Repository",
      id = Some("a11111111111111111111111"),
      rootFolderId  = "a11111111111111111111111",
      uncatFolderId = "a22222222222222222222222"),
    user = user)

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
    val user = User(Some("a11111111111111111111111"))

    request.setAttribute("user", user)
    MDC.put("USER", s"[${user.toString}]")
  }
}

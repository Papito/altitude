package software.altitude.core.controllers

import java.io.{PrintWriter, StringWriter}

import org.scalatra.InternalServerError
import org.slf4j.{LoggerFactory, MDC}
import software.altitude.core.models.{Repository, User}
import software.altitude.core.{Context, SingleApplication}

import scala.compat.Platform

abstract class BaseController extends AltitudeStack with SingleApplication {
  private final val log = LoggerFactory.getLogger(getClass)

  final def user = request.getAttribute("user").asInstanceOf[User]

  final def repository = request.getAttribute("repository").asInstanceOf[Repository]

  implicit lazy val context = new Context(repo = repository, user = user)

  before() {
    val requestId = software.altitude.core.Util.randomStr(size = 6)
    request.setAttribute("request_id", requestId)
    MDC.put("REQUEST_ID", s"[$requestId]")
    request.setAttribute("startTime", Platform.currentTime)
    setUser()
    setRepository()
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
    val userId = params("user_id")
    val user: User = app.service.user.getById(userId)
    MDC.put("USER", s"[$user]")
    request.setAttribute("user", user)
  }

  protected def setRepository() = {
    val repositoryId = params("repository_id")
    val repository: Repository = app.service.repository.getRepositoryById(repositoryId)
    MDC.put("REPO", s"[$repository]")
    request.setAttribute("repository", repository)
  }

  error {
    case ex: Exception =>
      ex.printStackTrace()
      val sw: StringWriter = new StringWriter()
      val pw: PrintWriter = new PrintWriter(sw)
      ex.printStackTrace(pw)
      log.error(s"Exception ${sw.toString}")
      InternalServerError(sw.toString)
  }
}

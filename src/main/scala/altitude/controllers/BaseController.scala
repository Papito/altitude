package altitude.controllers

import javax.servlet.http.HttpServletRequest

import altitude.SingleApplication
import org.slf4j.LoggerFactory

import scala.collection.mutable

abstract class BaseController extends AltitudeStack with SingleApplication {
  private final val log = LoggerFactory.getLogger(getClass)

  override protected def templateAttributes(implicit request: HttpServletRequest): mutable.Map[String, Any] = {
    super.templateAttributes ++ Map("config" -> app.config)
  }

  notFound {
    // remove content type in case it was set through an action
    contentType = null
    // Try to render a ScalateTemplate if no route matched
    findTemplate(requestPath) map { path =>
      contentType = "text/html"
      layoutTemplate(path)
    } orElse serveStaticResource() getOrElse resourceNotFound()
  }

  error {
    case ex: Exception => ex.printStackTrace()
  }

  before() {
    if (!isAssetUri)
      log.debug("Request START: " + request.getRequestURI)
  }

  after() {
    if (!isAssetUri)
      log.debug("Request END: " + request.getRequestURI)
  }

  private def isAssetUri: Boolean = {
    val uri = request.getRequestURI
    uri.startsWith("/js/") || uri.startsWith("/css/") || uri.startsWith("/i/")
  }
}

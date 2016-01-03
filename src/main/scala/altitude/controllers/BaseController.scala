package altitude.controllers

import javax.servlet.http.HttpServletRequest

import altitude.SingleApplication
import org.slf4j.LoggerFactory

import scala.collection.mutable
import scala.compat.Platform

abstract class BaseController extends AltitudeStack with SingleApplication {
  private final val log = LoggerFactory.getLogger(getClass)

  override protected def templateAttributes(implicit request: HttpServletRequest): mutable.Map[String, Any] = {
    super.templateAttributes ++ Map("config" -> app.config)
  }

  error {
    case ex: Exception => {
      ex.printStackTrace()
      this.status = 500
    }

  }

  before() {
    if (!isAssetUri && !isApiUri) {
      log.debug(s"Request START: ${request.getRequestURI}")
    }

    if (isApiUri) {
      request.setAttribute("startTime", Platform.currentTime)
    }
  }

  after() {
    if (!isAssetUri && !isApiUri) {
      log.debug(s"Request END: ${request.getRequestURI}")
    }

    if (isApiUri) {
      val startTime: Long = request.getAttribute("startTime").asInstanceOf[Long]
      log.debug(s"Request END: ${request.getRequestURI} in ${Platform.currentTime - startTime}ms")
    }
  }

  private def isAssetUri: Boolean = {
    val uri = request.getRequestURI
    //FIXME: clunky
    uri.startsWith("/js/") ||
      uri.startsWith("/css/") ||
      uri.startsWith("/i/") ||
      uri.startsWith("/assets/") ||
      uri.startsWith("/static/")
  }

  private def isApiUri = request.getRequestURI.startsWith("/api/")
}

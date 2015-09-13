package altitude.controllers

import javax.servlet.http.HttpServletRequest

import altitude.SingleApplication
import org.scalatra.{GZipSupport, ScalatraServlet}

import scala.collection.mutable

abstract class BaseController extends ScalatraServlet with AltitudeStack with SingleApplication {

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
}

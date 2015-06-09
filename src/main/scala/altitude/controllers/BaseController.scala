package altitude.controllers

import javax.servlet.http.HttpServletRequest

import altitude.SingleApplication
import org.scalatra.{ScalatraServlet}

import scala.collection.mutable

abstract class BaseController extends ScalatraServlet with AltitudeStack with SingleApplication {

  override protected def templateAttributes(implicit request: HttpServletRequest): mutable.Map[String, Any] = {
    super.templateAttributes ++ Map("config" -> app.config)
  }
}

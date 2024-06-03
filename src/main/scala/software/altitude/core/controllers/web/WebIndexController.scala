package software.altitude.core.controllers.web

import org.scalatra.ScalatraServlet
import org.scalatra.scalate.ScalateSupport
import org.slf4j.LoggerFactory

class WebIndexController extends ScalatraServlet with ScalateSupport{
  private final val log = LoggerFactory.getLogger(getClass)

  get("/") {
    mustache("/index")
  }
}

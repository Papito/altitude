package software.altitude.core.controllers.web

import org.scalatra.ScalatraServlet
import org.scalatra.scalate.ScalateSupport
import software.altitude.core.auth.AuthenticationSupport

class WebIndexController extends ScalatraServlet with ScalateSupport with AuthenticationSupport {

  before() {
    requireLogin()
  }

  get("/") {
    contentType = "text/html"
    mustache("/index")
  }
}

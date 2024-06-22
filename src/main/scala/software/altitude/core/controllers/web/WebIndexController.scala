package software.altitude.core.controllers.web

import org.scalatra.scalate.ScalateSupport
import software.altitude.core.AltitudeServletContext
import software.altitude.core.controllers.AltitudeStack

class WebIndexController extends AltitudeStack with ScalateSupport with AltitudeServletContext {

  before() {
    // requireLogin()
  }

  get("/") {
    // Kick to setup if this is a new install
    if (!app.isInitialized) {
      redirect("/setup")
    }

    contentType = "text/html"
    mustache("/index")
  }

  get("/setup") {
    contentType = "text/html"

    if (app.isInitialized) {
      redirect("/")
    } else
    mustache("/setup")
  }
}

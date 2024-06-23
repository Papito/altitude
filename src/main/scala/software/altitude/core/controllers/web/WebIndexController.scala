package software.altitude.core.controllers.web

import org.slf4j.LoggerFactory
import software.altitude.core.controllers.BaseWebController
import software.altitude.core.{Const => C}

class WebIndexController extends BaseWebController {
  private final val log = LoggerFactory.getLogger(getClass)

  before() {
    // requireLogin()
  }

  get("/") {
    // Kick to setup if this is a new install
    if (!app.isInitialized) {
      logger.warn("App is not initialized, redirecting to setup")
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
    mustache("/setup", "fields" -> C.Api.Fields, "constr" -> C.Api.Constraints)

  }
}

package altitude.controllers

import altitude.controllers.web.BaseWebController
import org.slf4j.LoggerFactory
import org.scalatra.Ok

class ClientController extends BaseWebController {
  private final val log = LoggerFactory.getLogger(getClass)

  get("/*") {
    val route = params("splat")
    log.info(s"route to $route")
    contentType = "text/html"
    Ok()
  }
}


package software.altitude.core.controllers.api.admin

import org.slf4j.LoggerFactory
import software.altitude.core.controllers.api.BaseApiController

class SetupController extends BaseApiController  {
  private final val log = LoggerFactory.getLogger(getClass)

  post("/") {
    if (app.isInitialized) {
      val message = "Instance is already initialized."
      log.warn(message)
      halt(400, message)
    }

    log.warn("Initializing up the instance...")
    response.addHeader("HX-Redirect", "/")
    OK
  }

}

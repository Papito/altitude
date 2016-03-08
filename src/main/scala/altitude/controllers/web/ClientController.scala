package altitude.controllers.web

import org.slf4j.LoggerFactory

class ClientController  extends BaseWebController {
  private final val log = LoggerFactory.getLogger(getClass)

  get("/*") {
    val path = this.params("splat")
    log.debug(s"Client file request: $path")
  }


}

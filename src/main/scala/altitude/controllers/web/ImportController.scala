package altitude.controllers.web

import org.slf4j.LoggerFactory

class ImportController extends BaseWebController {
  private final val log = LoggerFactory.getLogger(getClass)

  get("/") {
    contentType = "text/html"
    ssp("import")
  }
}


package altitude.controllers.web

import org.slf4j.LoggerFactory

class SearchController extends BaseWebController {
  private final val log = LoggerFactory.getLogger(getClass)

  get("/") {
    contentType = "text/html"
    ssp("search")
  }
}


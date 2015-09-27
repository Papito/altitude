package altitude.controllers.web

import altitude.SingleApplication._
import org.slf4j.LoggerFactory

class IndexController extends BaseWebController {
  private final val log = LoggerFactory.getLogger(getClass)

  get("/") {
    contentType = "text/html"
    ssp("index")
  }
}

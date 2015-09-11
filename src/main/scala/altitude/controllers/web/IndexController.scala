package altitude.controllers.web

import altitude.controllers.BaseController

class IndexController extends BaseWebController {

  get("/") {
    contentType = "text/html"
    ssp("/index")
  }
}

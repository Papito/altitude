package altitude.controllers.web

import altitude.controllers.BaseController

class IndexController extends BaseWebController {

  get("/") {
    contentType = "text/html"
    layoutTemplate("/WEB-INF/templates/views/index.ssp")
  }
}

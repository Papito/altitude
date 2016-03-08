package altitude.controllers.web

import org.slf4j.LoggerFactory

class IndexController extends BaseWebController {

  get("/") {
    redirect("/client/index")
  }
}

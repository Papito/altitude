package altitude.controllers

class IndexController extends BaseController {

  get("/") {
    contentType = "text/html"
    ssp("/index")
  }
}

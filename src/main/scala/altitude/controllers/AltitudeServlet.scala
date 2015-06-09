package altitude.controllers

class AltitudeServlet extends BaseController {

  get("/") {
    contentType="text/html"
    ssp("/index")
  }
}

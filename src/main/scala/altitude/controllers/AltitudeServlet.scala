package altitude.controllers

class AltitudeServlet extends AltitudeStack {

  get("/") {
    contentType="text/html"
    ssp("/index")
  }

}

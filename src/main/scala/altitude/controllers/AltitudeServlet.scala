package altitude.controllers

class AltitudeServlet extends AltitudeStack {

  get("/") {
    contentType="text/html"
    jade("/index")
  }

}

package altitude.controllers

class ImportServlet extends AltitudeStack {

  get("/") {
    contentType="text/html"
    jade("/import")
  }

}

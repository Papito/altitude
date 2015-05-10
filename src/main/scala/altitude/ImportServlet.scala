package altitude

class ImportServlet extends AltitudeStack {

  get("/") {
    contentType="text/html"
    jade("/import")
  }

}

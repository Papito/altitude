package altitude

class AltitudeServlet extends AltitudeStack {

  get("/") {
    contentType="text/html"
    jade("/hello-scalate")
  }

}

package altitude.controllers

import org.slf4j.LoggerFactory

class ImportServlet extends AltitudeStack {
  val log =  LoggerFactory.getLogger(getClass)

  get("/") {
    contentType="text/html"
    jade("/import")
  }
}

package altitude.controllers

import altitude.SingleApplication
import org.slf4j.LoggerFactory

class ImportServlet extends AltitudeStack with SingleApplication {
  val log = LoggerFactory.getLogger(getClass)

  get("/") {
    app.txManager.withTransaction {
      app.txManager.withTransaction {
        app.txManager.asReadOnly {
          log.info("I AM IN A TRANSACTION, BITCHES!")
        }
      }
    }

    contentType="text/html"
    jade("/import")
  }
}

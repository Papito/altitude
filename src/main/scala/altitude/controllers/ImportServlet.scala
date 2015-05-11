package altitude.controllers

import altitude.transactions.TransactionId
import altitude.{SingleApplication, Altitude}
import org.slf4j.LoggerFactory

class ImportServlet extends AltitudeStack with SingleApplication {
  val log = LoggerFactory.getLogger(getClass)

  get("/") {
    app.txManager.withTransaction {
      log.info("I AM IN A TRANSACTION, BITCHES!")
    }

    contentType="text/html"
    jade("/import")
  }
}

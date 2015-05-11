package altitude

import altitude.transactions.TransactionId
import org.slf4j.LoggerFactory

object SingleApplication {
  val log =  LoggerFactory.getLogger(getClass)
  log.info("Initializing single application... ")
  private val app: Altitude = new Altitude(isProd = true, isTest = false)
}

trait SingleApplication {
  val app = SingleApplication.app

  // create a new transaction id every time we start one
  implicit val txId: TransactionId = new TransactionId
}

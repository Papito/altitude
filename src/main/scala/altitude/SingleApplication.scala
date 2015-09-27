package altitude

import org.slf4j.LoggerFactory

object SingleApplication {
  private final val log = LoggerFactory.getLogger(getClass)
  log.info("Initializing single application... ")
  private val app: Altitude = new Altitude
}

trait SingleApplication {
  val app = SingleApplication.app
}

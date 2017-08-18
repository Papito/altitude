package software.altitude.core

import org.slf4j.LoggerFactory

/**
 * The singleton that makes sure we are only launching one instance of the app,
 * in a servlet environment.
 */
object SingleApplication {
  private final val log = LoggerFactory.getLogger(getClass)
  log.info("Initializing single application... ")
  private val app: Altitude = new Altitude
}

trait SingleApplication {
  val app = SingleApplication.app
}

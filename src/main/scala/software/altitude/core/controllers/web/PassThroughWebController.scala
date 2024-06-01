/*package software.altitude.core.controllers.web

import org.scalatra.InternalServerError
import org.slf4j.LoggerFactory
import software.altitude.core.SingleApplication
import software.altitude.core.controllers.AltitudeStack

import java.io.{PrintWriter, StringWriter}

/**
 * A version of web controller that does minimal logging and sets no context, to avoid
 * extra queries.
 *
 * Used for static asset controllers, redirects, etc.
 */
class PassThroughWebController extends AltitudeStack with SingleApplication {
  private final val log = LoggerFactory.getLogger(getClass)

  error {
    case ex: Exception =>
      ex.printStackTrace()
      val sw: StringWriter = new StringWriter()
      val pw: PrintWriter = new PrintWriter(sw)
      ex.printStackTrace(pw)
      log.error(s"Exception ${sw.toString}")
      InternalServerError(sw.toString)
  }
}
*/
